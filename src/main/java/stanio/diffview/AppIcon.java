/*
 * This module, both source code and documentation,
 * is in the Public Domain, and comes with NO WARRANTY.
 */
package stanio.diffview;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.image.AbstractMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import javax.swing.Icon;
import javax.swing.JPanel;

class AppIcon extends AbstractMultiResolutionImage implements Icon {

    /**
     * @see  https://ebourg.github.io/flamingo-svg-transcoder/
     */
    private static Shape iconShape;
    static {
        GeneralPath shape = new GeneralPath();
        shape.moveTo(6.0, 7.0);
        shape.lineTo(8.0, 7.0);
        shape.lineTo(8.0, 8.0);
        shape.lineTo(6.0, 8.0);
        shape.lineTo(6.0, 10.0);
        shape.lineTo(5.0, 10.0);
        shape.lineTo(5.0, 8.0);
        shape.lineTo(3.0, 8.0);
        shape.lineTo(3.0, 7.0);
        shape.lineTo(5.0, 7.0);
        shape.lineTo(5.0, 5.0);
        shape.lineTo(6.0, 5.0);
        shape.lineTo(6.0, 7.0);
        shape.closePath();
        shape.moveTo(3.0, 13.0);
        shape.lineTo(8.0, 13.0);
        shape.lineTo(8.0, 12.0);
        shape.lineTo(3.0, 12.0);
        shape.lineTo(3.0, 13.0);
        shape.closePath();
        shape.moveTo(7.5, 2.0);
        shape.lineTo(11.0, 5.5);
        shape.lineTo(11.0, 15.0);
        shape.curveTo(11.0, 15.55, 10.55, 16.0, 10.0, 16.0);
        shape.lineTo(1.0, 16.0);
        shape.curveTo(0.45, 16.0, 0.0, 15.55, 0.0, 15.0);
        shape.lineTo(0.0, 3.0);
        shape.curveTo(0.0, 2.45, 0.45, 2.0, 1.0, 2.0);
        shape.lineTo(7.5, 2.0);
        shape.closePath();
        shape.moveTo(10.0, 6.0);
        shape.lineTo(7.0, 3.0);
        shape.lineTo(1.0, 3.0);
        shape.lineTo(1.0, 15.0);
        shape.lineTo(10.0, 15.0);
        shape.lineTo(10.0, 6.0);
        shape.closePath();
        shape.moveTo(8.5, 0.0);
        shape.lineTo(3.0, 0.0);
        shape.lineTo(3.0, 1.0);
        shape.lineTo(8.0, 1.0);
        shape.lineTo(12.0, 5.0);
        shape.lineTo(12.0, 13.0);
        shape.lineTo(13.0, 13.0);
        shape.lineTo(13.0, 4.5);
        shape.lineTo(8.5, 0.0);
        shape.closePath();
        iconShape = shape;
    }

    @SuppressWarnings("serial")
    private Map<Dimension, BufferedImage>
            resolutions = new LinkedHashMap<>(4, 0.75f, true) {
        @Override protected
        boolean removeEldestEntry(Map.Entry<Dimension, BufferedImage> eldest) {
            return size() > 4;
        }
    };

    @Override
    public int getIconWidth() {
        return 16;
    }

    @Override
    public int getIconHeight() {
        return 16;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g.create();
            try {
                g2d.translate(x, y);
                paint(g2d, c);
            } finally {
                g2d.dispose();
            }
        } else {
            BufferedImage image = new BufferedImage(getIconWidth(), getIconHeight(),
                                                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            try {
                paint(g2d, c);
            } finally {
                g2d.dispose();
            }
            g.drawImage(image, x, y, null);
        }
    }

    void paint(Graphics2D g, Component c) {
        g.translate(2, 0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(getColor(c));
        g.fill(iconShape);
    }

    private static Color getColor(Component c) {
        return (c == null ? new JPanel() : c).getForeground();
    }

    @Override
    public int getWidth(ImageObserver observer) {
        return getIconWidth();
    }

    @Override
    public int getHeight(ImageObserver observer) {
        return getIconHeight();
    }

    @Override
    protected Image getBaseImage() {
        return getResolutionVariant(getIconWidth(), getIconHeight());
    }

    @Override
    public Image getResolutionVariant(double destWidth,
                                      double destHeight) {
        Dimension key = new Dimension((int) Math.ceil(destWidth),
                                      (int) Math.ceil(destHeight));
        return resolutions.computeIfAbsent(key, k -> {
            BufferedImage img = new BufferedImage(k.width, k.height,
                                                  BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            try {
                g.scale((double) k.width / getIconWidth(),
                        (double) k.height / getIconHeight());
                paint(g, null);
            } finally {
                g.dispose();
            }
            return img;
        });
    }

    @Override
    public List<Image> getResolutionVariants() {
        if (resolutions.isEmpty()) {
            return Collections.singletonList(getBaseImage());
        }
        return resolutions.values().stream().sorted((img1, img2) -> {
            int diff = img1.getWidth() - img2.getWidth();
            if (diff == 0) {
                diff = img1.getHeight() - img2.getHeight();
            }
            return diff;
        }).collect(Collectors.toList());
    }

}
