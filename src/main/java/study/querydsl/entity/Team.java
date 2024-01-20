package study.querydsl.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자를 만들어줌 - protected로 .
@ToString(of = {"id", "name"}) // 연관관계 필드들은 손 대지마랏!
public class Team {

    @Id
    @GeneratedValue
    private Long id;
    private String name;

    // 연관관계 주인 x  // 외래키 값을 업데이트 x
    @OneToMany(mappedBy = "team")
    private List<Member> members = new ArrayList<>();


    public Team(String name) {
        this.name = name;
    }



}
