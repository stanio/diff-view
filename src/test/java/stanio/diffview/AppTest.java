/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class AppTest {

    @Test
    public void windowTitle() {
        DiffView window = new DiffView();
        assertEquals(window.getTitle(), "diff-view", "Window title");
    }

}
