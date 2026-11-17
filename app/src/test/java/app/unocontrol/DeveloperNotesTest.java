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
        assertEquals("Original note and four new subjects", 5, notes.length);
        assertEquals("Не доверяйте ИИ на слово", notes[0].title);
        assertEquals("Почему появился GitTeller", notes[1].title);
        assertEquals("Ошибки, которым меня научила разработка", notes[2].title);
        assertEquals("Мои исследования за пределами GitTeller", notes[3].title);
        assertEquals("Как ChatGPT приписала себе непроверенные тесты", notes[4].title);
        for (int i = 0; i < notes.length; i++) {
            for (int j = i + 1; j < notes.length; j++) {
                assertNotEquals(notes[i].title, notes[j].title);
            }
        }
        assertEquals(notes[0], DeveloperNotes.at(0));
        assertEquals(notes[3], DeveloperNotes.at(3));
        assertEquals(notes[4], DeveloperNotes.at(4));
    }

    @Test public void assistantIncidentIsAttributedAndAuthorized() {
        DeveloperNotes.Entry incident = DeveloperNotes.at(4);
        assertNotNull(incident);
        assertTrue(incident.subtitle.contains("ChatGPT"));
        assertTrue(incident.subtitle.contains("разрешения владельца"));
        StringBuilder narrative = new StringBuilder();
        for (DeveloperNotes.Section section : incident.sections) {
            narrative.append(section.title).append(' ').append(section.text).append(' ');
        }
        String text = narrative.toString();
        assertTrue(text.contains("я, ChatGPT"));
        assertTrue(text.contains("6 из 6 JVM-сценариев"));
        assertTrue(text.contains("нет команд запуска"));
        assertTrue(text.contains("тест написан"));
        assertTrue(text.contains("тест выполнен"));
        assertTrue(text.contains("Samsung Galaxy A52"));
    }
}
