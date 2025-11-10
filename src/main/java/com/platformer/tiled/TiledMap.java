package com.platformer.tiled;

import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;

/**
 * Loader TMX/TSX sederhana (Visual + Solid + Objects).
 * Tambahan: dukungan slope & object 'enemy' -> spawn list.
 */
public class TiledMap {

    // ==== public API ====
    public int getCols() { return cols; }
    public int getRows() { return rows; }
    public int getTile() { return tile; }
    public Rectangle2D getDoor() { return doorRect; }
    public double getSpawnX() { return spawnX; }
    public double getSpawnY() { return spawnY; }
    public boolean showColliderOverlay = false;

    // NEW: daftar spawn musuh (pixel top-left untuk lingkaran enemy)
    public List<double[]> getEnemySpawns() { return Collections.unmodifiableList(enemySpawns); }

    // slope
    public enum Slope { NONE, UP_RIGHT, UP_LEFT }

    public boolean isSolid(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= cols || ty >= rows) return false;
        return solid[ty][tx] != 0;
    }
    public Slope slopeAt(int tx, int ty) {
        if (tx < 0 || ty < 0 || tx >= cols || ty >= rows) return Slope.NONE;
        int gid = solid[ty][tx];
        if (gid <= 0) return Slope.NONE;
        return slopeByGid.getOrDefault(gid, Slope.NONE);
    }

    public void render(GraphicsContext g) {
        g.setFill(Color.web("#426B7F"));
        g.fillRect(0, 0, cols * tile, rows * tile);

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int gid = visual[y][x];
                if (gid > 0) drawGid(g, gid, x * tile, y * tile);
            }
        }

        if (doorGid > 0) {
            drawGid(g, doorGid, doorDx, doorDy);
        } else if (doorRect != null) {
            g.setFill(Color.web("#FFD400"));
            g.fillRect(doorRect.getMinX(), doorRect.getMinY(), doorRect.getWidth(), doorRect.getHeight());
        }

        if (showColliderOverlay) {
            g.setGlobalAlpha(0.25);
            g.setFill(Color.RED);
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    if (solid[y][x] != 0) g.fillRect(x * tile, y * tile, tile, tile);
                }
            }
            g.setGlobalAlpha(1.0);
        }
    }

    // ==== construction / loading ====

    public TiledMap(String tmxClasspath) { loadTmx(tmxClasspath); }

    private int cols, rows, tile;
    private int[][] visual;
    private int[][] solid;

    private double spawnX = 80, spawnY = 80;
    private Rectangle2D doorRect;
    private int doorGid = -1; private double doorDx = 0, doorDy = 0;

    // NEW: enemy spawns
    private final List<double[]> enemySpawns = new ArrayList<>();

    private static class Tileset {
        int firstGid;
        boolean spritesheet;
        int tileWidth, tileHeight, margin, spacing;
        Image sheet; int columns;
        Map<Integer, Image> imagesByLocalId = new HashMap<>();
    }
    private final Map<Integer, Tileset> tsByFirstGid = new HashMap<>();
    private final Map<Integer, Tileset> tsForGid = new HashMap<>();
    private final Map<Integer, Slope> slopeByGid = new HashMap<>();

    private void loadTmx(String tmxRes) {
        try {
            InputStream is = tryRes(tmxRes);
            if (is == null) is = tryRes("/assets" + tmxRes);
            if (is == null) throw new RuntimeException("TMX tidak ditemukan: " + tmxRes);

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            Element map = doc.getDocumentElement();

            cols = Integer.parseInt(map.getAttribute("width"));
            rows = Integer.parseInt(map.getAttribute("height"));
            tile = Integer.parseInt(map.getAttribute("tilewidth"));

            NodeList tsList = map.getElementsByTagName("tileset");
            for (int i = 0; i < tsList.getLength(); i++) {
                Element tsEl = (Element) tsList.item(i);
                int firstGid = Integer.parseInt(tsEl.getAttribute("firstgid"));
                String source = tsEl.getAttribute("source");
                Tileset ts = new Tileset(); ts.firstGid = firstGid;
                if (source != null && !source.isEmpty()) {
                    parseTsx("/maps/", source, ts);
                } else {
                    parseTilesetElement(tsEl, ts, "/maps/");
                }
                tsByFirstGid.put(firstGid, ts);
            }

            visual = new int[rows][cols];
            solid  = new int[rows][cols];

            NodeList layers = map.getElementsByTagName("layer");
            for (int i = 0; i < layers.getLength(); i++) {
                Element layer = (Element) layers.item(i);
                String name = layer.getAttribute("name");
                Element data = (Element) layer.getElementsByTagName("data").item(0);
                String encoding = data.getAttribute("encoding");
                if (!"csv".equalsIgnoreCase(encoding)) {
                    throw new RuntimeException("Hanya encoding CSV yang didukung");
                }
                String csv = data.getTextContent().trim();
                int[][] target = "Solid".equalsIgnoreCase(name) ? solid : visual;
                parseCsvInto(csv, target);
            }

            // object layers
            NodeList ogNodes = map.getElementsByTagName("objectgroup");
            for (int i = 0; i < ogNodes.getLength(); i++) {
                Element og = (Element) ogNodes.item(i);
                NodeList objs = og.getElementsByTagName("object");
                for (int j = 0; j < objs.getLength(); j++) {
                    Element o = (Element) objs.item(j);
                    String nm = o.getAttribute("name");
                    if (nm == null) nm = "";
                    String name = nm.toLowerCase();

                    double ox = getDoubleOr(o, "x", 0);
                    double oy = getDoubleOr(o, "y", 0);

                    if ("spawn".equals(name) || "player".equals(name)) {
                        spawnX = ox;
                        spawnY = oy - tile;
                        continue;
                    }
                    if ("door".equals(name)) {
                        double w = getDoubleOr(o, "width", 0);
                        double h = getDoubleOr(o, "height", 0);
                        if (o.hasAttribute("gid")) {
                            int gid = Integer.parseInt(o.getAttribute("gid"));
                            int[] sz = sizeForGid(gid);
                            if (w <= 0) w = sz[0];
                            if (h <= 0) h = sz[1];
                            doorGid = gid; doorDx = ox; doorDy = oy - h;
                        } else {
                            if (w <= 0) w = tile;
                            if (h <= 0) h = 2 * tile;
                            doorGid = -1; doorDx = ox; doorDy = oy - h;
                        }
                        doorRect = new Rectangle2D(ox, oy - h, w, h);
                        continue;
                    }
                    // NEW: enemy spawn (titik). Jika object tile, pakai oy - tinggi? karena tile object y = bottom.
                    if ("enemy".equals(name)) {
                        double ex = ox;
                        double ey = oy - tile; // kira-kira sejajar grid
                        enemySpawns.add(new double[]{ex, ey});
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Gagal memuat TMX: " + tmxRes + " ∙ " + e.getMessage(), e);
        }
    }

    private void parseTsx(String baseFolder, String tsxRel, Tileset ts) throws Exception {
        String tsxRes = resolveFrom(baseFolder, tsxRel);
        InputStream is = tryRes(tsxRes);
        if (is == null) is = tryRes("/assets" + tsxRes);
        if (is == null && tsxRes.startsWith("/maps/")) {
            String file = tsxRes.substring(tsxRes.lastIndexOf('/') + 1);
            is = tryRes("/tiles/" + file);
            if (is == null) is = tryRes("/assets/tiles/" + file);
        }
        if (is == null) throw new RuntimeException("TSX tidak ditemukan: " + tsxRes);

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        Element tsEl = doc.getDocumentElement();
        parseTilesetElement(tsEl, ts, tsxRes.substring(0, tsxRes.lastIndexOf('/') + 1));
    }

    /** Baca elemen <tileset> (spritesheet / collection); sekaligus baca properti "slope". */
    private void parseTilesetElement(Element tsEl, Tileset ts, String basePath) {
        NodeList imageNodes = tsEl.getElementsByTagName("image");
        NodeList tileNodes  = tsEl.getElementsByTagName("tile");

        ts.tileWidth  = getInt(tsEl, "tilewidth", 32);
        ts.tileHeight = getInt(tsEl, "tileheight", 32);
        ts.margin     = getInt(tsEl, "margin", 0);
        ts.spacing    = getInt(tsEl, "spacing", 0);

        if (imageNodes.getLength() > 0 && tileNodes.getLength() == 0) {
            Element img = (Element) imageNodes.item(0);
            String src = resolveFrom(basePath, img.getAttribute("source"));
            ts.sheet = loadImage(src);
            ts.columns = getInt(tsEl, "columns",
                    (int) Math.max(1, Math.floor((ts.sheet.getWidth() - ts.margin * 2 + ts.spacing) /
                                                 (ts.tileWidth + ts.spacing))));
            ts.spritesheet = true;
        } else {
            ts.spritesheet = false;
        }

        for (int i = 0; i < tileNodes.getLength(); i++) {
            Element tileEl = (Element) tileNodes.item(i);
            int localId = Integer.parseInt(tileEl.getAttribute("id"));

            if (!ts.spritesheet) {
                Element imgEl = (Element) tileEl.getElementsByTagName("image").item(0);
                if (imgEl != null) {
                    String src = resolveFrom(basePath, imgEl.getAttribute("source"));
                    ts.imagesByLocalId.put(localId, loadImage(src));
                }
            }

            Element props = (Element) tileEl.getElementsByTagName("properties").item(0);
            if (props != null) {
                NodeList plist = props.getElementsByTagName("property");
                for (int p = 0; p < plist.getLength(); p++) {
                    Element prop = (Element) plist.item(p);
                    String name = prop.getAttribute("name");
                    if ("slope".equalsIgnoreCase(name)) {
                        String val = prop.getAttribute("value");
                        Slope s = "upright".equalsIgnoreCase(val) ? Slope.UP_RIGHT :
                                  "upleft".equalsIgnoreCase(val)  ? Slope.UP_LEFT  : Slope.NONE;
                        if (s != Slope.NONE) {
                            int gid = ts.firstGid + localId;
                            slopeByGid.put(gid, s);
                        }
                    }
                }
            }
        }
    }

    private void parseCsvInto(String csv, int[][] target) {
        String[] rowsStr = csv.split("\\s*\\n\\s*");
        for (int y = 0; y < rows; y++) {
            String[] nums = rowsStr[y].split("\\s*,\\s*");
            for (int x = 0; x < cols; x++) {
                int gid = Integer.parseInt(nums[x]);
                target[y][x] = gid;
            }
        }
    }

    private void drawGid(GraphicsContext g, int gid, double dx, double dy) {
        Tileset ts = tilesetForGid(gid);
        if (ts == null) return;
        int local = gid - ts.firstGid;
        if (ts.spritesheet) {
            int tw = ts.tileWidth, th = ts.tileHeight;
            int col = local % ts.columns;
            int row = local / ts.columns;
            int sx = ts.margin + col * (tw + ts.spacing);
            int sy = ts.margin + row * (th + ts.spacing);
            g.drawImage(ts.sheet, sx, sy, tw, th, dx, dy, tw, th);
        } else {
            Image img = ts.imagesByLocalId.get(local);
            if (img != null) g.drawImage(img, dx, dy);
        }
    }

    private int[] sizeForGid(int gid) {
        Tileset ts = tilesetForGid(gid);
        if (ts == null) return new int[]{tile, tile};
        if (ts.spritesheet) return new int[]{ts.tileWidth, ts.tileHeight};
        Image img = ts.imagesByLocalId.get(gid - ts.firstGid);
        if (img != null) return new int[]{(int) img.getWidth(), (int) img.getHeight()};
        return new int[]{tile, tile};
    }

    private Tileset tilesetForGid(int gid) {
        Tileset cached = tsForGid.get(gid);
        if (cached != null) return cached;
        Tileset best = null; int bestFirst = -1;
        for (Map.Entry<Integer, Tileset> e : tsByFirstGid.entrySet()) {
            int first = e.getKey();
            if (gid >= first && first > bestFirst) { best = e.getValue(); bestFirst = first; }
        }
        if (best != null) tsForGid.put(gid, best);
        return best;
    }

    private static InputStream tryRes(String path) { return TiledMap.class.getResourceAsStream(path); }
    private static Image loadImage(String classpath) {
        InputStream is = tryRes(classpath);
        if (is == null) is = tryRes("/assets" + classpath);
        if (is == null) throw new RuntimeException("Gambar tidak ditemukan: " + classpath);
        return new Image(is);
    }
    private static String resolveFrom(String base, String rel) {
        if (rel.startsWith("/")) return rel;
        String path = base + rel;
        while (path.contains("/./")) path = path.replace("/./", "/");
        while (path.contains("../")) {
            int i = path.indexOf("../");
            if (i <= 0) break;
            int slash = path.lastIndexOf('/', i - 2);
            if (slash >= 0) path = path.substring(0, slash + 1) + path.substring(i + 3);
            else path = path.substring(i + 3);
        }
        if (!path.startsWith("/")) path = "/" + path;
        return path;
    }
    private static int getInt(Element el, String attr, int def) {
        return el.hasAttribute(attr) ? Integer.parseInt(el.getAttribute(attr)) : def;
    }
    private static double getDoubleOr(Element el, String attr, double def) {
        return el.hasAttribute(attr) ? Double.parseDouble(el.getAttribute(attr)) : def;
    }
}
