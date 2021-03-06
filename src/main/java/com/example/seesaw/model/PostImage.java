package com.example.seesaw.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String postImages;

    @ManyToOne //(fetch = FetchType.LAZY)
    @JoinColumn(name = "postid", nullable = false)
    private Post post;

    public PostImage(String postImages, Post post){
        this.postImages = postImages;
        this.post = post;
    }

}
