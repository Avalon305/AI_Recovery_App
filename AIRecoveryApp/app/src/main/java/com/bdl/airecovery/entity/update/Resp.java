package com.bdl.airecovery.entity.update;

public class Resp {
    private String version;
    private  String publicUrl;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public Resp(String version, String publicUrl) {
        this.version = version;
        this.publicUrl = publicUrl;
    }
}
