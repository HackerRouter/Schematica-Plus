package com.github.lunatrius.schematica.client.world;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class SchematicLibraryTest {

    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private File source(String name, int value) throws IOException {
        File file = new File(temporary.getRoot(), name);
        Files.write(file.toPath(), new byte[] {(byte) value});
        return file;
    }

    private SchematicLibrary<byte[], byte[]> library() {
        return new SchematicLibrary<>(file -> Files.readAllBytes(file.toPath()));
    }

    @Test public void loadingCanonicalSourceDoesNotCreateAPlacementOrReadItTwice() throws IOException {
        SchematicLibrary<byte[], byte[]> library = library();
        File file = source("a.schematic", 1);
        SchematicLibrary.Source<byte[]> loaded = library.load(file);
        Files.write(file.toPath(), new byte[] {2});
        assertSame(loaded, library.load(new File(temporary.getRoot(), "./a.schematic")));
        assertEquals(1, loaded.data()[0]);
        assertEquals(1, library.sources().size());
        assertTrue(library.placements().isEmpty());
    }

    @Test public void placementsRemainIndependentAndRemovingLastOneRetainsSource() throws IOException {
        SchematicLibrary<byte[], byte[]> library = library();
        SchematicLibrary.Source<byte[]> source = library.load(source("a.schematic", 1));
        byte[] first = library.create(source, (data, previous) -> data.clone());
        byte[] second = library.create(source, (data, previous) -> data.clone());
        first[0] = 42;
        assertEquals(1, second[0]);
        assertEquals(1, source.data()[0]);
        assertSame(source, library.sourceOf(first));
        library.remove(first);
        library.remove(second);
        assertTrue(library.placements().isEmpty());
        assertEquals(1, library.sources().size());
        Files.delete(source.file().toPath());
        assertEquals(1, library.create(source, (data, previous) -> data.clone())[0]);
    }

    @Test public void unloadRemovesOnlyTheSourcesPlacementsAndRejectsStaleActions() throws IOException {
        SchematicLibrary<byte[], byte[]> library = library();
        SchematicLibrary.Source<byte[]> first = library.load(source("a.schematic", 1));
        SchematicLibrary.Source<byte[]> second = library.load(source("b.schematic", 2));
        byte[] one = library.create(first, (data, previous) -> data.clone());
        byte[] two = library.create(second, (data, previous) -> data.clone());
        assertEquals(java.util.Collections.singletonList(one), library.unload(first));
        assertEquals(java.util.Collections.singletonList(two), library.placements());
        assertThrows(IllegalArgumentException.class, () -> library.create(first, (data, previous) -> data.clone()));
        library.clear();
        assertTrue(library.sources().isEmpty());
        assertTrue(library.placements().isEmpty());
        assertNull(library.sourceOf(two));
    }

    @Test public void reloadStagesEveryReplacementBeforeChangingAnything() throws IOException {
        SchematicLibrary<byte[], byte[]> library = library();
        SchematicLibrary.Source<byte[]> source = library.load(source("a.schematic", 1));
        byte[] one = library.create(source, (data, previous) -> data.clone());
        byte[] two = library.create(source, (data, previous) -> data.clone());
        Files.write(source.file().toPath(), new byte[] {2});
        assertThrows(IOException.class, () -> library.reload(source, (data, previous) -> {
            if (previous == two) throw new IOException("Cannot transform second placement");
            return data.clone();
        }));
        assertEquals(1, source.data()[0]);
        assertSame(one, library.placements().get(0));
        assertSame(two, library.placements().get(1));
        assertSame(source, library.sourceOf(one));
        Map<byte[], byte[]> changes = library.reload(source, (data, previous) -> data.clone());
        assertEquals(2, source.data()[0]);
        assertEquals(2, library.placements().get(0)[0]);
        assertSame(changes.get(two), library.placements().get(1));
        assertNull(library.sourceOf(one));
        assertSame(source, library.sourceOf(changes.get(one)));
    }

    @Test public void missingReloadSourcePreservesPlacementsAndRenamePreservesOwnership() throws IOException {
        SchematicLibrary<byte[], byte[]> library = library();
        File old = source("a.schematic", 3).getCanonicalFile();
        SchematicLibrary.Source<byte[]> source = library.load(old);
        byte[] placement = library.create(source, (data, previous) -> data.clone());
        File renamed = new File(temporary.getRoot(), "renamed.schematic");
        Files.move(old.toPath(), renamed.toPath());
        library.renamed(old, renamed);
        assertSame(source, library.load(renamed));
        assertSame(source, library.sourceOf(placement));
        Files.delete(renamed.toPath());
        assertThrows(IOException.class, () -> library.reload(source, (data, previous) -> data.clone()));
        assertSame(placement, library.placements().get(0));
        assertEquals(3, source.data()[0]);
    }

    @Test public void failedLoadAndFailedCreationDoNotPublishPartialState() throws IOException {
        SchematicLibrary<byte[], byte[]> library = library();
        assertThrows(IOException.class, () -> library.load(new File(temporary.getRoot(), "missing.schematic")));
        assertTrue(library.sources().isEmpty());
        SchematicLibrary.Source<byte[]> source = library.load(source("a.schematic", 1));
        assertThrows(IOException.class, () -> library.create(source, (data, previous) -> { throw new IOException("Invalid data"); }));
        assertTrue(library.placements().isEmpty());
        assertEquals(1, library.sources().size());
    }

    @Test public void reloadKeepsUnrelatedPlacementsInOrderAndDoesNotCreateOneForAnUnplacedSource() throws IOException {
        SchematicLibrary<byte[], byte[]> library = library();
        SchematicLibrary.Source<byte[]> first = library.load(source("a.schematic", 1));
        SchematicLibrary.Source<byte[]> second = library.load(source("b.schematic", 2));
        byte[] one = library.create(first, (data, previous) -> data.clone());
        byte[] other = library.create(second, (data, previous) -> data.clone());
        byte[] two = library.create(first, (data, previous) -> data.clone());
        Map<byte[], byte[]> changes = library.reload(first, (data, previous) -> data.clone());
        assertEquals(2, changes.size());
        assertSame(changes.get(one), library.placements().get(0));
        assertSame(other, library.placements().get(1));
        assertSame(changes.get(two), library.placements().get(2));
        library.remove(other);
        assertTrue(library.reload(second, (data, previous) -> { throw new AssertionError("Unexpected placement"); }).isEmpty());
        assertEquals(2, library.placements().size());
        assertEquals(2, library.sources().size());
    }

    @Test public void renameCannotTakeThePathOfAnotherCachedSourceWhoseFileWasDeleted() throws IOException {
        SchematicLibrary<byte[], byte[]> library = library();
        SchematicLibrary.Source<byte[]> first = library.load(source("a.schematic", 1));
        SchematicLibrary.Source<byte[]> second = library.load(source("b.schematic", 2));
        File original = first.file();
        Files.delete(second.file().toPath());
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> library.checkRename(original, second.file()));
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> library.renamed(original, second.file()));
        assertEquals(original, first.file());
        assertEquals(1, first.data()[0]);
        assertEquals(2, second.data()[0]);
        library.unload(second);
        library.checkRename(original, second.file());
    }

    @Test public void refreshRecreatesPlacementsFromMemoryWithoutRereadingTheFile() throws IOException {
        SchematicLibrary<byte[], byte[]> library = library();
        File file = source("a.schematic", 1);
        SchematicLibrary.Source<byte[]> source = library.load(file);
        SchematicLibrary.Source<byte[]> other = library.load(source("b.schematic", 9));
        byte[] first = library.create(source, (data, previous) -> data.clone());
        byte[] unrelated = library.create(other, (data, previous) -> data.clone());
        byte[] second = library.create(source, (data, previous) -> data.clone());
        source.data()[0] = 5;
        Files.write(file.toPath(), new byte[] {7});
        Map<byte[], byte[]> changes = library.refresh(source, (data, previous) -> data.clone());
        assertEquals(2, changes.size());
        assertEquals(5, changes.get(first)[0]);
        assertSame(changes.get(first), library.placements().get(0));
        assertSame(unrelated, library.placements().get(1));
        assertSame(changes.get(second), library.placements().get(2));
        assertEquals(5, source.data()[0]);
        assertSame(source, library.sourceOf(changes.get(second)));
        assertNull(library.sourceOf(first));
    }

    @Test public void memorySourcesAreNeverMatchedOrRenamedAsFiles() throws IOException {
        SchematicLibrary<byte[], byte[]> library = library();
        File file = source("a.schematic", 1);
        SchematicLibrary.Source<byte[]> memory = library.addMemory(file, new byte[] {9});
        assertTrue(memory.memory());
        SchematicLibrary.Source<byte[]> loaded = library.load(file);
        assertNotSame(memory, loaded);
        assertFalse(loaded.memory());
        assertEquals(1, loaded.data()[0]);
        File renamed = new File(temporary.getRoot(), "b.schematic");
        library.renamed(file.getCanonicalFile(), renamed);
        assertEquals(renamed.getCanonicalFile(), loaded.file());
        assertEquals(file.getAbsoluteFile(), memory.file());
        assertEquals(2, library.sources().size());
    }
}
