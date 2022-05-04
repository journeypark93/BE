package com.example.seesaw.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class UserProfile {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id; //1, 2,....

    @Column
    private String category; //헤어1, 헤어2, 헤어3, 눈썹1, 눈썹2, 눈썹3, 머리1, 머리2, 머리3

    @Column
    private String imageUrl; //s3 url, s3 url, s3 url, s3 url, s3 url, s3 url, s3 url, s3 url,

//    @ManyToOne
//    @JoinColumn(name = "USER_ID", nullable = false)
//    private User user;

}
