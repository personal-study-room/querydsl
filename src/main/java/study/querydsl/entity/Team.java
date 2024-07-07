package study.querydsl.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "teamName"})
public class Team {


  @Id
  @GeneratedValue
  @Column(name = "team_id")
  private Long id;

  private String teamName;

  @OneToMany(mappedBy = "team")
  private List<Member> members = new ArrayList<>();

  public Team(String teamName) {
    this.teamName = teamName;
  }
}
