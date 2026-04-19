package com.atakmap.android.maps;

import com.atakmap.api.Point2;
import com.atakmap.api.map.MapItem;

import java.util.HashMap;
import java.util.Map;

public class MapView {

    private final RootGroup rootGroup = new RootGroup();
    private final Map<String, MapItem> items = new HashMap<>();

    public MapItem getMapItem(String uid) {
        return items.get(uid);
    }

    public RootGroup getRootGroup() {
        return rootGroup;
    }

    public void invalidate() {
    }

    public Point2 forward(Point2 point) {
        return point;
    }

    public Point2 toScreen(Point2 point) {
        return point;
    }

    public Point2 toPixels(Point2 point) {
        return point;
    }

    public Point2 projectToScreen(Point2 point) {
        return point;
    }

    public final class RootGroup {
        public void addItem(MapItem item) {
            if (item != null && item.getUid() != null) {
                items.put(item.getUid(), item);
            }
        }
    }
}
