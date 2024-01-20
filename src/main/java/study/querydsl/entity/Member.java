package study.querydsl.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자를 만들어줌 - protected로 .
@ToString(of = {"id", "username", "age"}) // 연관관계 필드들은 손 대지마랏!
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;


    // 연관관계 주인 0
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") // column 이름
    private Team team;


    public Member(String username) {
        this(username, 0);
    }

    public Member(String username, int age) {
        this(username, age, null);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }

}
