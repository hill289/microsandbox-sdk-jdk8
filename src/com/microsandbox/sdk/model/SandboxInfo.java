package com.microsandbox.sdk.model;

/**
 * Sandbox summary information from list/lookup operations.
 * <p>JDK 8 compatible.</p>
 */
public class SandboxInfo {

    private String name;
    private String id;
    private SandboxStatus status;
    private String image;

    public SandboxInfo() {}

    public SandboxInfo(String name, String id, SandboxStatus status, String image) {
        this.name = name;
        this.id = id;
        this.status = status;
        this.image = image;
    }

    public String getName() { return name; }
    public String getId() { return id; }
    public SandboxStatus getStatus() { return status; }
    public String getImage() { return image; }

    public void setName(String name) { this.name = name; }
    public void setId(String id) { this.id = id; }
    public void setStatus(SandboxStatus status) { this.status = status; }
    public void setImage(String image) { this.image = image; }

    @Override
    public String toString() {
        return "SandboxInfo{name='" + name + "', id='" + id
                + "', status=" + status + ", image='" + image + "'}";
    }

    /**
     * Creates SandboxInfo from JSON fields.
     */
    public static SandboxInfo fromJson(String json) {
        SandboxInfo info = new SandboxInfo();
        info.name = extractField(json, "name");
        info.id = extractField(json, "id");
        String statusStr = extractField(json, "status");
        info.status = SandboxStatus.fromWireValue(statusStr);
        info.image = extractField(json, "image");
        return info;
    }

    private static String extractField(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int qStart = json.indexOf('"', colon + 1);
        if (qStart < 0) return null;
        int qEnd = qStart + 1;
        while (qEnd < json.length()) {
            char c = json.charAt(qEnd);
            if (c == '\\') { qEnd += 2; continue; }
            if (c == '"') break;
            qEnd++;
        }
        return json.substring(qStart + 1, qEnd);
    }
}
