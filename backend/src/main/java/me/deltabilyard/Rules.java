package me.deltabilyard;
import java.util.*;
public final class Rules {
 public record Result(boolean foul,String message,int winner,boolean continues,int assignedGroup,boolean rerack){}
 static int group(int id){return id>=1&&id<=7?1:id>=9&&id<=15?2:0;}
 static boolean cleared(List<Physics.Ball>b,int group){return b.stream().noneMatch(a->!a.pocketed&&group(a.id)==group);}
 public static Result evaluate(List<Physics.Ball> before,Physics.Shot shot,int team,int assigned,boolean breaking,int calledPocket){
  List<Physics.Event> pots=shot.events().stream().filter(e->e.type().equals("pocket")).sorted(Comparator.comparingDouble(Physics.Event::t)).toList();
  boolean scratch=shot.balls().get(0).pocketed;boolean eight=pots.stream().anyMatch(e->e.ball()==8);
  int first=shot.firstContact();boolean ready=assigned!=0&&cleared(before,assigned);
  boolean wrong=first<0||(breaking?first!=1:assigned==0?first==8:ready?first!=8:group(first)!=assigned);
  long breakRails=shot.events().stream().filter(e->e.type().equals("rail")&&e.ball()!=0).map(Physics.Event::ball).distinct().count();
  boolean noRail=pots.isEmpty()&&!shot.railAfterContact();boolean illegalBreak=breaking&&pots.stream().noneMatch(e->e.ball()!=0)&&breakRails<4;
  boolean foul=scratch||wrong||noRail||illegalBreak;
  String reason=scratch?"биток в лузе":wrong?(first<0?"биток не коснулся шара":breaking?"на разбое первым нужен шар №1":assigned==0?"восьмёрку нельзя касаться первой на открытом столе":ready?"первым нужно попасть по восьмёрке":"первым задет чужой шар №"+first):illegalBreak?"на разбое ни одного шара в лузе и менее четырёх шаров у борта":"после контакта нет ни борта, ни забитого шара";
  if(breaking&&eight)return new Result(foul,"Восьмёрка на разбое — новая расстановка"+(foul?". Фол: "+reason:"; разбивает тот же игрок"),-1,!foul,0,true);
  if(eight){
   boolean called=pots.stream().anyMatch(e->e.ball()==8&&e.pocket()==calledPocket);int winner=ready&&!foul&&called?team:1-team;
   String message=winner==team?"Восьмёрка в заказанной лузе — партия выиграна":!ready?"Поражение: восьмёрка забита до своей группы":foul?"Поражение на восьмёрке: "+reason:"Поражение: восьмёрка попала не в заказанную лузу";
   return new Result(foul,message,winner,false,assigned,false);
  }
  int assignedNow=assigned;
  if(!breaking&&!foul&&assigned==0)assignedNow=pots.stream().mapToInt(e->group(e.ball())).filter(g->g!=0).findFirst().orElse(0);
  final int g=assignedNow;boolean keep=!foul&&pots.stream().anyMatch(e->e.ball()!=0&&(breaking||g==0||group(e.ball())==g));
  String message=foul?"Фол: "+reason+" — ход соперника, биток с руки":keep?(breaking?"Шар забит на разбое — серия продолжается":"Свой шар забит — серия продолжается"):pots.isEmpty()?"Нет забитого шара — ход соперника":"Забит только чужой шар — ход соперника";
  return new Result(foul,message,-1,keep,assignedNow,false);
 }
 static int lowest(List<Physics.Ball> balls){return balls.stream().filter(b->b.id>0&&!b.pocketed).mapToInt(b->b.id).min().orElse(-1);}
 static List<Integer> pocketedObjects(Physics.Shot shot){return shot.events().stream().filter(e->e.type().equals("pocket")&&e.ball()>0).map(Physics.Event::ball).distinct().toList();}
 public static Result evaluate(String ruleSet,List<Physics.Ball> before,Physics.Shot shot,int team,int assigned,boolean breaking,int calledPocket,int points,int pointsToWin){
  if(ruleSet.equals("eight"))return evaluate(before,shot,team,assigned,breaking,calledPocket);
  boolean nine=ruleSet.equals("nine"),scratch=shot.balls().get(0).pocketed;
  List<Integer> pots=pocketedObjects(shot);int first=shot.firstContact(),required=nine?lowest(before):breaking?1:-1;
  boolean wrong=first<0||(required>0&&first!=required);
  long rails=shot.events().stream().filter(e->e.type().equals("rail")&&e.ball()>0).map(Physics.Event::ball).distinct().count();
  boolean illegalBreak=breaking&&pots.isEmpty()&&rails<4;
  boolean noRail=pots.isEmpty()&&!shot.railAfterContact();
  boolean foul=scratch||wrong||illegalBreak||noRail;
  if(foul){
   String reason=scratch?"биток в лузе":wrong?(first<0?"биток не коснулся шара":"первым нужен шар №"+required):illegalBreak?"на разбое ни одного шара в лузе и менее четырёх шаров у борта":"после контакта нет ни борта, ни забитого шара";
   String spot=nine?(pots.contains(9)?"; девятка возвращается на стол":""):pots.isEmpty()?"":"; забитые шары возвращаются на стол";
   return new Result(true,"Фол: "+reason+spot+" — ход соперника, биток с руки",-1,false,0,false);
  }
  boolean win=nine?pots.contains(9):points+pots.size()>=pointsToWin;
  if(win)return new Result(false,nine?"Девятка забита правильно — партия выиграна":pointsToWin==15?"Все 15 шаров забиты — тренировка завершена":"Забито "+(points+pots.size())+" шаров — партия выиграна",team,false,0,false);
  return new Result(false,pots.isEmpty()?"Нет забитого шара — ход соперника":"Шар забит — серия продолжается",-1,!pots.isEmpty(),0,false);
 }

}
