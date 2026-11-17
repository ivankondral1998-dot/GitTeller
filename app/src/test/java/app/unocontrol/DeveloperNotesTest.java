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

    @Test public void distinctTopicsArePresent() {
        DeveloperNotes.Entry[] notes = DeveloperNotes.all();
        assertEquals("Original note and three new subjects", 4, notes.length);
        assertEquals("Не доверяйте ИИ на слово", notes[0].title);
        assertEquals("Почему появился GitTeller", notes[1].title);
        assertEquals("Ошибки, которым меня научила разработка", notes[2].title);
        assertEquals("Мои исследования за пределами GitTeller", notes[3].title);
        for (int i = 0; i < notes.length; i++) {
            for (int j = i + 1; j < notes.length; j++) {
                assertNotEquals(notes[i].title, notes[j].title);
            }
        }
        assertEquals(notes[0], DeveloperNotes.at(0));
        assertEquals(notes[3], DeveloperNotes.at(3));
    }
}
