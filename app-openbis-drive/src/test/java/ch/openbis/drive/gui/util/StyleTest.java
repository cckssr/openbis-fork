package ch.openbis.drive.gui.util;

import javafx.scene.paint.Color;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Path;
import java.util.List;

@RunWith(JUnit4.class)
public class StyleTest {
    @Test
    public void testToCssValue() {
        for (String colour : List.of(
                "#ffffb600",
                "#bb5ebbff",
                "#541b63aa",
                "#00af9845",
                "#32227c55",
                "#4443ff01",
                "#0853f010")) {
            Assert.assertEquals(colour, Style.toCssValue(Color.valueOf(colour)));
        }
    }
}
