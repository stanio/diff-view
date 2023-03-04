/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import static stanio.diffview.Colors.*;

import java.util.Arrays;
import java.util.Collections;
import java.awt.Color;

import javax.swing.UIManager;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

class DiffStyles {

    static interface Name {
        String DELETED_LINE = "deleted-line";
        String INSERTED_LINE = "inserted-line";
        String DELETED_NUMBER = "deleted-number";
        String INSERTED_NUMBER = "inserted-number";
        String MESSAGE = "message-text";
        String DIFF_COMMAND = "diff-command";
        String FROM_FILE = "from-file";
        String TO_FILE = "to-file";
        String HUNK = "hunk";
        String HUNK_LABEL = "hunk-label";
    }

    public static void main(String[] args) throws Exception {
        for (Object key : Collections
                .list(UIManager.getLookAndFeel().getDefaults().keys())) {
            Object value = UIManager.get(key);
            if (value instanceof Color) {
                System.out.println(key + "=#" + Integer
                        .toHexString(((Color) value).getRGB()).toUpperCase());
            }
        }
    }

    static void addTo(StyledDocument context) {
        addStylesOf(context, colorOf(229,  83,  75, 0.15f),
                             colorOf(229,  83,  75, 0.4f),
                             colorOf( 70, 149,  74, 0.15f),
                             colorOf( 70, 149,  74, 0.4f),
                             colorOf(220, 189, 251, 1f),
                             UIManager.getColor(""),
                             colorOf(108, 182, 255, 1f));
    }

    private static void addStylesOf(StyledDocument context,
                                    Color deletion,
                                    Color deletionHilite,
                                    Color addition,
                                    Color additionHilite,
                                    Color hunk,
                                    Color hunkTitle,
                                    Color diffCommand) {
        addStyleTo(context, Name.INSERTED_NUMBER, Color.WHITE, additionHilite);
        addStyleTo(context, Name.DELETED_NUMBER, Color.WHITE, deletionHilite);
        addStyleTo(context, Name.INSERTED_LINE, null, addition);
        addStyleTo(context, Name.DELETED_LINE, null, deletion);

        Color yellowHighlight = new Color(1f, 1f, 0, .05f);
        addStyleTo(context, Name.FROM_FILE, new Color(deletionHilite.getRGB()), null);
        addStyleTo(context, Name.TO_FILE, new Color(additionHilite.getRGB()), null);
        addStyleTo(context, Name.DIFF_COMMAND, hunk, null);
        addStyleTo(context, Name.HUNK, diffCommand, yellowHighlight);
        StyleConstants.setItalic(addStyleTo(context,
                Name.HUNK_LABEL, UIManager.getColor("textInactiveText"), null), true);
        addStyleTo(context, Name.MESSAGE, UIManager.getColor("textInactiveText"), null);
    }

    private static Style addStyleTo(StyledDocument context, String name, Color fg, Color bg) {
        Style style = context.addStyle(name, context.getStyle(StyleContext.DEFAULT_STYLE));
        if (fg != null) StyleConstants.setForeground(style, fg);
        if (bg != null) StyleConstants.setBackground(style, bg);
        return style;
    }

}


class Colors {

    private static final Color TRANSPARENT = new Color(0, true);

    private static final double K_B = 0.0593; // ITU-R BT.2020 conversion
    private static final double K_R = 0.2627; // https://en.wikipedia.org/wiki/YCbCr#ITU-R_BT.2020_conversion
    private static final double K_G = 1 - K_B - K_R;

    private static final double SCALE_B = 2 * (1 - K_B);
    private static final double SCALE_R = 2 * (1 - K_R);
    private static final double SCALE_GB = SCALE_B * K_B / K_G;
    private static final double SCALE_GR = SCALE_R * K_R / K_G;

    static float getLuma(Color col) {
        // Y
        return (float) (K_R * gamma1(col.getRed())
                        + K_G * gamma1(col.getGreen())
                        + K_B * gamma1(col.getBlue()));
    }

    static float[] getYCbCr(Color col) {
        // https://en.wikipedia.org/wiki/YCbCr#YCbCr
        double y = getLuma(col); // KR * R + KG * G + KB * B
        double cB = ((col.getRGB() & 0xFF) - y) / SCALE_B; // (1 / 2) * (B − Y) / (1 − KB)
        double cR = (((col.getRGB() >> 16) & 0xFF) - y) / SCALE_R; // (1 / 2) * (R − Y) / (1 − KR)

        float[] yCbCr = new float[3];
        yCbCr[0] = (float) y;
        yCbCr[1] = (float) cB;
        yCbCr[2] = (float) cR;
        if (yCbCr.length > 3) {
            yCbCr[3] = col.getAlpha();
        }
        return yCbCr;
    }

    static Color colorOfYCbCr(float[] yCbCr) {
        float y = yCbCr[0];
        float cB = yCbCr[1];
        float cR = yCbCr[2];

        int r = clamp(Math.round(y + cR * SCALE_R));
        int g = clamp(Math.round(y - cB * SCALE_GB - cR * SCALE_GR));
        int b = clamp(Math.round(y + cB * SCALE_B));

        if (yCbCr.length > 3) {
            int a = (int) Math.round(yCbCr[3]);
            return new Color(r, g, b, a);
        }
        return new Color(r, g, b);
    }

    private static int clamp(double val) {
        return (int) Math.round((val < 0) ? 0 : (val > 255) ? 255 : val);
    }

    private static double gamma1(int val) {
        return gamma(val, 1.0);
    }

    private static double gamma(int val, double gamma) {
        return Math.pow(val / 255.0, gamma);
    }

    // find intermediate color between two colors with alpha channels (=> NO alpha blending!!!)
    static Color blend(int M, int N, Color pixBack, Color pixFront) {
        assert (0 < M && M < N && N <= 1000);

        final int weightFront = pixFront.getAlpha() * M;
        final int weightBack = pixBack.getAlpha() * (N - M);
        final int weightSum = weightFront + weightBack;
        if (weightSum == 0)
            return TRANSPARENT;

        return new Color(calcColor(weightFront, weightBack, weightSum,
                                   pixFront.getRed(), pixBack.getRed()),
                         calcColor(weightFront, weightBack, weightSum,
                                   pixFront.getGreen(), pixBack.getGreen()),
                         calcColor(weightFront, weightBack, weightSum,
                                   pixFront.getBlue(), pixBack.getBlue()),
                         weightSum / N);
    }

    private static int calcColor(int weightFront, int weightBack, int weightSum,
                                 int colFront, int colBack) {
        return (colFront * weightFront + colBack * weightBack) / weightSum;
    }

    /**
     * @see  <a href="https://developer.mozilla.org/en-US/docs/Web/CSS/color_value/rgba"
     *          >rgba()</a> <i>(CSS)</i>
     */
    static Color colorOf(int r, int g, int b, float a) {
        return new Color(r, g, b, (int) (a * 255 + 0.5));
    }

    static Color withAlpha(Color color, float alpha) {
        int newAlpha = (int) (color.getAlpha() * alpha) << 24;
        return new Color(color.getRGB() & 0xFFFFFF | newAlpha, true);
    }

    public static void main(String[] args) {
        float[] yCbCr = getYCbCr(new Color(0xC080FF));
        System.out.println(Arrays.toString(yCbCr));
        System.out.println(Integer
                .toHexString(colorOfYCbCr(yCbCr).getRGB()).toUpperCase());
    }

}
