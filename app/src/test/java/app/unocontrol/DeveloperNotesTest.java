package app.unocontrol;

import org.junit.Test;
import static org.junit.Assert.*;

/** Offline author-note regression checks, no network or Android runtime. */
public class DeveloperNotesTest {
    @Test public void notesHaveStableTitlesAndBoundedSize() {
        DeveloperNotes.Entry[] notes = DeveloperNotes.all();
        assertTrue(notes.length > 0);
        for (DeveloperNotes.Entry note : notes) {
            assertFalse(note.title.isEmpty());
            assertFalse(note.subtitle.isEmpty());
            assertTrue(note.sections.length > 0);
            int total = 0;
            for (DeveloperNotes.Section section : note.sections) {
                assertFalse(section.title.isEmpty());
                assertFalse(section.text.isEmpty());
                total += section.title.length() + section.text.length();
            }
            assertTrue("Keep each note short enough for a few pages", total < 7000);
        }
        assertNull(DeveloperNotes.at(-1));
        assertNull(DeveloperNotes.at(notes.length));
    }
}
