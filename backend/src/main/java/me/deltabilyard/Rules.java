package me.deltabilyard;
import java.util.*;
public final class Rules {
 public record Result(boolean foul,String message,int winner,boolean continues,int assignedGroup,boolean rerack){}
 static int group(int id){return id>=1&&id<=7?1:id>=9&&id<=15?2:0;}
 static boolean cleared(List<Physics.Ball>b,int group){return b.stream().noneMatch(a->!a.pocketed&&group(a.id)==group);}
 public static Result evaluate(List<Physics.Ball> before,Physics.Shot shot,int team,int assigned,boolean breaking,int calledPocket){
  List<Physics.Event> pots=shot.events().stream().filter(e->e.type().equals("pocket")).toList();
  boolean scratch=shot.balls().get(0).pocketed;boolean eight=pots.stream().anyMatch(e->e.ball()==8);
  int first=shot.firstContact();boolean ready=assigned!=0&&cleared(before,assigned);
  boolean wrong=first<0||(breaking?first!=1:assigned==0?first==8:ready?first!=8:group(first)!=assigned);
  long breakRails=shot.events().stream().filter(e->e.type().equals("rail")&&e.ball()!=0).map(Physics.Event::ball).distinct().count();
  boolean noRail=pots.isEmpty()&&!shot.railAfterContact();boolean illegalBreak=breaking&&pots.stream().noneMatch(e->e.ball()!=0)&&breakRails<4;
  boolean foul=scratch||wrong||noRail||illegalBreak;
  if(breaking&&eight)return new Result(foul,"Восьмёрка на разбое — новая расстановка",-1,!foul,0,true);
  if(eight){boolean called=pots.stream().anyMatch(e->e.ball()==8&&e.pocket()==calledPocket);int winner=!breaking&&ready&&!foul&&called?team:1-team;return new Result(foul,winner==team?"Восьмёрка! Партия выиграна":"Неправильная восьмёрка — поражение",winner,false,assigned,false);}
  int assignedNow=assigned;
  if(!breaking&&!foul&&assigned==0)assignedNow=pots.stream().mapToInt(e->group(e.ball())).filter(g->g!=0).findFirst().orElse(0);
  final int g=assignedNow;boolean keep=!foul&&pots.stream().anyMatch(e->e.ball()!=0&&(breaking||g==0||group(e.ball())==g));
  String message=foul?(scratch?"Фол: биток в лузе":wrong?"Фол: неверный первый контакт":illegalBreak?"Фол: недостаточный разбой":"Фол: нет борта после контакта"):pots.isEmpty()?"Ход переходит сопернику":"Шар забит!";
  return new Result(foul,message,-1,keep,assignedNow,false);
 }
}
