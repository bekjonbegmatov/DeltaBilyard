package me.deltabilyard;

import java.util.*;

/** Bounded, deterministic planning against the same physics and rules as a human shot. */
final class ExpertBot {
 static final int MAX_PREVIEWS=80;
 record Search(AimBot.Plan plan,int simulations){}
 private record Candidate(double angle,double power,int pocket,int target,double cost,int kind){}
 private record Trial(Candidate candidate,AimBot.Plan plan,double utility){}
 private static final double D=Physics.R*2, E=Physics.R;
 private ExpertBot(){}

 static Search search(List<Physics.Ball> balls,int group,boolean breaking,String ruleSet){
  List<Candidate> candidates=new ArrayList<>();
  if(breaking){
   Physics.Ball cue=balls.get(0),one=balls.get(1);double angle=Math.atan2(one.y-cue.y,one.x-cue.x);
   for(double power:new double[]{.78,.88,.98})for(double offset:new double[]{0,.008})candidates.add(new Candidate(angle+offset,power,-1,1,0,0));
  }else{
   addBest(candidates,direct(balls,group,ruleSet),12);
   if(ruleSet.equals("nine"))addBest(candidates,combinations(balls),4);
   addBest(candidates,banks(balls,group,ruleSet),6);
   addBest(candidates,contacts(balls,group,ruleSet,false),4);
   addBest(candidates,contacts(balls,group,ruleSet,true),4);
  }
  // Every normal position has a legal target. Keep a finite ordinary shot for corrupted/finished snapshots.
  if(candidates.isEmpty()){
   var p=AimBot.choose(balls,group,breaking,"pro",0,ruleSet);
   candidates.add(new Candidate(p.angle(),p.power(),p.pocket(),p.target(),100,3));
  }
  List<Trial> trials=new ArrayList<>();int count=0;
  for(Candidate c:candidates){trials.add(trial(balls,group,breaking,ruleSet,c,c.angle,c.power,0));count++;}
  trials.sort(Comparator.comparingDouble(Trial::utility).reversed());
  Trial best=trials.get(0);
  if(!breaking){
   // Refine promising pots and legal defensive shots; no random error or hidden state changes.
   List<Trial> refine=new ArrayList<>(trials.subList(0,Math.min(7,trials.size())));
   for(Trial base:refine){
    Candidate c=base.candidate;
    double[][] variations={{0,.82,0},{0,1.18,0},{0,1,-.65},{0,1,.5},{-.003,1,0},{.003,1,0},{-.006,1,0},{.006,1,0}};
    for(double[] v:variations){
     if(count>=MAX_PREVIEWS)break;
     Trial t=trial(balls,group,false,ruleSet,c,c.angle+v[0],clamp(c.power*v[1]),v[2]);count++;
     if(t.utility>best.utility)best=t;
    }
   }
  }
  return new Search(best.plan,count);
 }

 private static Trial trial(List<Physics.Ball> balls,int group,boolean breaking,String rules,Candidate c,double angle,double power,double spin){
  Physics.Shot shot=Physics.preview(balls,angle,power,0,spin);
  Rules.Result result=Rules.evaluate(rules,balls,shot,0,group,breaking,c.pocket,0,8);
  double score;
  if(result.winner()==0)score=1_000_000;
  else if(result.winner()==1)score=-1_000_000;
  else if(result.foul())score=-100_000+(shot.firstContact()==c.target?100:0)-(shot.balls().get(0).pocketed?500:0);
  else {
   List<Integer> pots=Rules.pocketedObjects(shot);
   int assigned=result.assignedGroup()!=0?result.assignedGroup():group;
   long own=pots.stream().filter(id->!rules.equals("eight")||breaking||assigned==0||Rules.group(id)==assigned).count();
   score=(result.continues()?10_000:1_000)+own*1000-(pots.size()-own)*50;
   if(!breaking){
    if(result.continues())score+=opportunity(shot.balls(),assigned,rules);
    else score-=opportunity(shot.balls(),rules.equals("eight")&&assigned!=0?3-assigned:0,rules)*.8;
   }
   Physics.Ball cue=shot.balls().get(0);
   double edge=Math.min(Math.min(cue.x,Physics.W-cue.x),Math.min(cue.y,Physics.H-cue.y));
   score-=Math.max(0,.13-edge)*100;
  }
  // Among otherwise similar outcomes prefer a ball close to its intended pocket and controlled pace.
  Physics.Ball target=shot.balls().get(c.target);
  if(!target.pocketed&&c.pocket>=0){double[] p=Physics.POCKETS[c.pocket];score-=Math.hypot(target.x-p[0],target.y-p[1])*35;}
  score-=power*3+Math.abs(spin)*.3+c.cost*.01;
  return new Trial(c,new AimBot.Plan(angle,power,c.pocket,c.target,-score,0,spin),score);
 }

 private static double opportunity(List<Physics.Ball> balls,int group,String rules){
  List<Candidate> next=direct(balls,group,rules);
  if(next.isEmpty())return 0;
  double best=next.stream().mapToDouble(Candidate::cost).min().orElse(20);
  return 450/(1+best)+Math.min(4,next.size())*20;
 }
 private static void addBest(List<Candidate> output,List<Candidate> input,int count){input.sort(Comparator.comparingDouble(Candidate::cost));output.addAll(input.subList(0,Math.min(count,input.size())));}
 private static boolean legal(Physics.Ball b,List<Physics.Ball> balls,int group,String rules){return AimBot.legal(b,balls,group,group!=0&&Rules.cleared(balls,group),false,rules);}
 private static double clamp(double power){return Math.max(.08,Math.min(.98,power));}
 private static double power(double distance){return clamp(Physics.powerForSpeed(Math.sqrt(.46*distance+.20)));}
 private static boolean inside(double x,double y){return x>=E&&x<=Physics.W-E&&y>=E&&y<=Physics.H-E;}
 private static boolean clear(List<Physics.Ball> balls,double x,double y,double tx,double ty,int... excluded){
  double dx=tx-x,dy=ty-y,len=dx*dx+dy*dy;
  outer:for(Physics.Ball b:balls){if(b.pocketed)continue;for(int id:excluded)if(b.id==id)continue outer;
   double t=len<1e-12?0:Math.max(0,Math.min(1,((b.x-x)*dx+(b.y-y)*dy)/len));
   if(Math.hypot(b.x-x-t*dx,b.y-y-t*dy)<D*1.015)return false;
  }return true;
 }
 private static Candidate aimed(List<Physics.Ball> balls,Physics.Ball ball,double tx,double ty,double travel,int pocket,double penalty,int kind){
  Physics.Ball cue=balls.get(0);double dx=tx-ball.x,dy=ty-ball.y,length=Math.hypot(dx,dy);if(length<1e-8)return null;
  double gx=ball.x-dx/length*D,gy=ball.y-dy/length*D,cx=gx-cue.x,cy=gy-cue.y,approach=Math.hypot(cx,cy);
  if(!inside(gx,gy)||approach<1e-8||!clear(balls,cue.x,cue.y,gx,gy,0,ball.id))return null;
  double cosine=(cx*dx+cy*dy)/(approach*length);if(cosine<.22)return null;
  double distance=approach+travel/(cosine*cosine);
  return new Candidate(Math.atan2(cy,cx),power(distance),pocket,ball.id,approach*.55+travel+(1-cosine)*2+penalty,kind);
 }
 private static List<Candidate> direct(List<Physics.Ball> balls,int group,String rules){
  List<Candidate> plans=new ArrayList<>();
  for(Physics.Ball b:balls){if(!legal(b,balls,group,rules))continue;
   for(int i=0;i<6;i++){double[] p=Physics.POCKETS[i];if(!clear(balls,b.x,b.y,p[0],p[1],0,b.id))continue;
    Candidate c=aimed(balls,b,p[0],p[1],Math.hypot(p[0]-b.x,p[1]-b.y),i,0,0);if(c!=null)plans.add(c);
   }
  }return plans;
 }
 /** Restitution-corrected single cushion point for a moving ball centre. */
 private static double[] bounce(double x,double y,double tx,double ty,int wall){
  boolean vertical=wall<2;double axis=wall==0?E:wall==1?Physics.W-E:wall==2?E:Physics.H-E;
  double start=vertical?x:y,end=vertical?tx:ty;
  if(Math.abs(end-axis)<1e-6)return null;
  double k=-.82*(axis-start)/(end-axis);if(k<=0)return null;
  double other=((vertical?y:x)+k*(vertical?ty:tx))/(1+k);
  double bx=vertical?axis:other,by=vertical?other:axis;
  if(!inside(bx,by))return null;
  if(vertical?(by<.12||by>Physics.H-.12):(bx<.12||bx>Physics.W-.12||Math.abs(bx-Physics.W/2)<.13))return null;
  return new double[]{bx,by};
 }
 private static List<Candidate> banks(List<Physics.Ball> balls,int group,String rules){
  List<Candidate> plans=new ArrayList<>();
  for(Physics.Ball b:balls){if(!legal(b,balls,group,rules))continue;
   for(int pocket=0;pocket<6;pocket++)for(int wall=0;wall<4;wall++){
    double[] p=Physics.POCKETS[pocket],q=bounce(b.x,b.y,p[0],p[1],wall);if(q==null)continue;
    if(!clear(balls,b.x,b.y,q[0],q[1],0,b.id)||!clear(balls,q[0],q[1],p[0],p[1],0,b.id))continue;
    double travel=Math.hypot(q[0]-b.x,q[1]-b.y)+Math.hypot(p[0]-q[0],p[1]-q[1])/ .67;
    Candidate c=aimed(balls,b,q[0],q[1],travel,pocket,2,1);if(c!=null)plans.add(c);
   }
  }return plans;
 }
 private static List<Candidate> combinations(List<Physics.Ball> balls){
  List<Candidate> plans=new ArrayList<>();int first=Rules.lowest(balls);if(first<1||first==9||balls.get(9).pocketed)return plans;
  Physics.Ball b=balls.get(first),nine=balls.get(9);
  for(int pocket=0;pocket<6;pocket++){
   double[] p=Physics.POCKETS[pocket];double dx=p[0]-nine.x,dy=p[1]-nine.y,len=Math.hypot(dx,dy);if(len<1e-8)continue;
   double gx=nine.x-dx/len*D,gy=nine.y-dy/len*D,ax=gx-b.x,ay=gy-b.y,travel=Math.hypot(ax,ay);
   if(travel<D||!inside(gx,gy)||!clear(balls,b.x,b.y,gx,gy,0,first,9)||!clear(balls,nine.x,nine.y,p[0],p[1],0,first,9))continue;
   double cosine=(ax*dx+ay*dy)/(travel*len);if(cosine<.45)continue;
   Candidate c=aimed(balls,b,gx,gy,travel+len/(cosine*cosine),pocket,-2,2);if(c!=null)plans.add(c);
  }return plans;
 }
 private static List<Candidate> contacts(List<Physics.Ball> balls,int group,String rules,boolean kick){
  List<Candidate> plans=new ArrayList<>();Physics.Ball cue=balls.get(0);
  for(Physics.Ball b:balls){if(!legal(b,balls,group,rules))continue;
   int pocket=0;double nearest=100;
   for(int i=0;i<6;i++){double d=Math.hypot(b.x-Physics.POCKETS[i][0],b.y-Physics.POCKETS[i][1]);if(d<nearest){nearest=d;pocket=i;}}
   if(!kick&&clear(balls,cue.x,cue.y,b.x,b.y,0,b.id)){
    double angle=Math.atan2(b.y-cue.y,b.x-cue.x),distance=Math.hypot(b.x-cue.x,b.y-cue.y);
    for(double offset:new double[]{0,-.025,.025})plans.add(new Candidate(angle+offset,Math.max(.35,power(distance+1.3)),pocket,b.id,distance+5,3));
   }
   if(kick)for(int wall=0;wall<4;wall++){
    double[] q=bounce(cue.x,cue.y,b.x,b.y,wall);if(q==null||!clear(balls,cue.x,cue.y,q[0],q[1],0,b.id)||!clear(balls,q[0],q[1],b.x,b.y,0,b.id))continue;
    double distance=Math.hypot(q[0]-cue.x,q[1]-cue.y)+Math.hypot(b.x-q[0],b.y-q[1])/ .67;
    plans.add(new Candidate(Math.atan2(q[1]-cue.y,q[0]-cue.x),power(distance+1.3),pocket,b.id,distance+7,4));
   }
  }return plans;
 }

 static double[] placement(List<Physics.Ball> balls,int group,boolean breaking,String rules){
  if(breaking)return AimBot.placement(balls,group,true,rules);
  double[] best=null;double cost=Double.POSITIVE_INFINITY;
  for(Physics.Ball b:balls){if(!legal(b,balls,group,rules))continue;
   for(double[] pocket:Physics.POCKETS){double dx=pocket[0]-b.x,dy=pocket[1]-b.y,d=Math.hypot(dx,dy);if(d<.001)continue;
    for(double gap:new double[]{.18,.28,.42}){
     double x=b.x-dx/d*gap,y=b.y-dy/d*gap;
     if(!Physics.canPlace(balls,x,y)||!clear(balls,x,y,b.x,b.y,0,b.id)||!clear(balls,b.x,b.y,pocket[0],pocket[1],0,b.id))continue;
     double score=d+gap*.2+Math.max(0,.12-Math.min(Math.min(x,Physics.W-x),Math.min(y,Physics.H-y)))*4;
     if(score<cost){cost=score;best=new double[]{x,y};}
    }
   }
  }
  return best!=null?best:AimBot.placement(balls,group,false,rules);
 }
}
