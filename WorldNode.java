public class WorldNode {
    private final String name;
    private final String type;
    private final int x;
    private final int y;
    private final String description;

    public WorldNode(String name, String type, int x, int y, String description) {
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getDescription() {
        return description;
    }
}
