package com.animediscs.model;

import com.animediscs.model.disc.Disc;
import com.animediscs.support.BaseModel;
import com.animediscs.support.Constants;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "disc_list")
public class DiscList extends BaseModel implements Comparable<DiscList> {

    private String name;
    private String title;

    private Date date;
    private List<Disc> discs = new LinkedList<>();

    @Column(length = 100, nullable = false, unique = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(length = 100, nullable = false)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Column
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @ManyToMany
    @JoinTable(name = "disc_list_discs")
    public List<Disc> getDiscs() {
        return discs;
    }

    public void setDiscs(List<Disc> discs) {
        this.discs = discs;
    }

    public int compareTo(DiscList other) {
        Assert.notNull(other);
        if (Constants.TOP_100_NAME.equals(name)) return -1;
        if (Constants.TOP_100_NAME.equals(other.name)) return 1;
        return other.name.compareTo(name);
    }

}