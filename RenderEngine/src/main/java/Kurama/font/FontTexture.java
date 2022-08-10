package Kurama.font;

import Kurama.Math.Vector;
import Kurama.Mesh.Texture;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;

public class FontTexture {

    public class CharInfo {

        public int startX;
        public int width;
        public Vector texUL;
        public Vector texBL;
        public Vector texUR;
        public Vector texBR;

        public CharInfo(int startX, int width) {
            this.startX = startX;
            this.width = width;
        }

    }

    public Font font;
    public String charSetName;
    public Texture texture;
    public Map<Character, CharInfo> charMap;
    private static final String IMAGE_FORMAT = "png";
    private static final int CHAR_PADDING = 2;
    public int width;
    public int height;
    public static String defaultCharSet = "ISO-8859-1";

    public FontTexture(Font font, String charSetName) {
        this.font = font;
        this.charSetName = charSetName;
        charMap = new HashMap<>();
        try {
            buildTexture();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildTexture() throws Exception {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2D = img.createGraphics();
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setFont(font);
        FontMetrics fontMetrics = g2D.getFontMetrics();

        String allChars = getAllAvailableChars(charSetName);
        this.width = 0;
        this.height = fontMetrics.getHeight();
        for (char c : allChars.toCharArray()) {  // Get the size for each character and update global image size
            CharInfo charInfo = new CharInfo(width, fontMetrics.charWidth(c));
            charMap.put(c, charInfo);
            width += charInfo.width + CHAR_PADDING;
        }

        //Set texture coordinates
        for(var charInfo: charMap.values()) {
            charInfo.texBL = new Vector(new float[]{(float)charInfo.startX / (float)width, 0, 0, 0});
            charInfo.texUL = new Vector(new float[]{(float)charInfo.startX / (float)width,1, 0, 0});
            charInfo.texBR = new Vector(new float[]{((float)charInfo.startX + charInfo.width) / (float)width,0, 0, 0});
            charInfo.texUR = new Vector(new float[]{((float)charInfo.startX + charInfo.width) / (float)width,1, 0, 0});
        }

        g2D.dispose();

        // Create the image associated to the charset
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2D = img.createGraphics();
        g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2D.setFont(font);
        fontMetrics = g2D.getFontMetrics();
        g2D.setColor(Color.WHITE);
        int startX = 0;
        for (char c : allChars.toCharArray()) {
            CharInfo charInfo = charMap.get(c);
            g2D.drawString("" + c, startX, fontMetrics.getAscent());
            startX += charInfo.width + CHAR_PADDING;
        }
        g2D.dispose();

        ByteBuffer buf = null;
        try ( ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, IMAGE_FORMAT, out);
            out.flush();
            byte[] data = out.toByteArray();
            buf = ByteBuffer.allocateDirect(data.length);
            buf.put(data, 0, data.length);
            buf.flip();
        } catch (IOException e) {
            e.printStackTrace();
        }
        texture = Texture.createTexture(buf);

    }

    public String getAllAvailableChars(String charSetName) {
        CharsetEncoder ce = Charset.forName(charSetName).newEncoder();
        StringBuilder result = new StringBuilder();
        for (char c = 0; c < Character.MAX_VALUE; c++) {
            if (ce.canEncode(c)) {
                result.append(c);
            }
        }
        return result.toString();
    }

    public CharInfo getCharInfo(char c) {
        return charMap.get(c);
    }

}
