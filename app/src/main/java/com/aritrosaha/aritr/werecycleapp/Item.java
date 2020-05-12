package com.aritrosaha.aritr.werecycleapp;

public class Item {
    private String name;
    private String info;
    private String type;

    private String img;

    public Item() {}

    public Item(String img, String name, String type, String info)
    {
        // Do some extra preparation on the name string because in database it is lowercase
        this.name = CustomTools.capitailizeWord(name);
        this.info = info;
        this.img = img;
        this.type = type;
    }

    public String getName() {
        // TODO: Extremely inefficient, find another way
        return CustomTools.capitailizeWord(name);
    }

    public String getInfo() {
        return info;
    }

    public String getType() {
        return type;
    }

    public String getImg() {
        return img;
    }
}
