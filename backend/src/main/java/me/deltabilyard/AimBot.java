package me.deltabilyard;

import java.util.*;

/** Bounded geometric planning; never searches or simulates whole game trees. */
final class AimBot {
 record Plan(double angle,double power,int pocket,int target,double score,double spinX,double spinY){
  Plan(double angle,double power,int pocket,int target,double score){this(angle,power,pocket,target,score,0,0);}
 }
 private static final double CLEARANCE=Physics.R*2.02;
 static boolean legal(Physics.Ball b,int group,boolean cleared,boolean breaking){
  return !b.pocketed&&b.id>0&&(breaking?b.id==1:cleared?b.id==8:group==0?b.id!=8:Rules.group(b.id)==group);
 }
 static Plan choose(List<Physics.Ball> balls,int group,boolean breaking,String level,long seed){return choose(balls,group,breaking,level,seed,"eight");}
 static boolean legal(Physics.Ball b,List<Physics.Ball> balls,int group,boolean cleared,boolean breaking,String ruleSet){
  if(ruleSet.equals("eight"))return legal(b,group,cleared,breaking);
  return !b.pocketed&&b.id>0&&(breaking?b.id==1:ruleSet.equals("nine")?b.id==Rules.lowest(balls):true);
 }
 static Plan choose(List<Physics.Ball> balls,int group,boolean breaking,String level,long seed,String ruleSet){
  if(level.equals("expert"))return ExpertBot.search(balls,group,breaking,ruleSet).plan();
  Physics.Ball cue=balls.get(0);boolean cleared=group!=0&&Rules.cleared(balls,group);
  Random random=new Random(seed);
  if(breaking)return new Plan(Math.atan2(balls.get(1).y-cue.y,balls.get(1).x-cue.x)+noise(level,random)*.15,level.equals("beginner")?.67:.88,-1,1,0);
  List<Plan> candidates=new ArrayList<>();
  for(Physics.Ball ball:balls){
   if(!legal(ball,balls,group,cleared,false,ruleSet))continue;
   for(int pocket=0;pocket<Physics.POCKETS.length;pocket++){
    double[] p=Physics.POCKETS[pocket];double dx=p[0]-ball.x,dy=p[1]-ball.y,length=Math.hypot(dx,dy);
    if(length<1e-7)continue;
    double gx=ball.x-dx/length*Physics.R*2,gy=ball.y-dy/length*Physics.R*2;
    if(gx<Physics.R||gx>Physics.W-Physics.R||gy<Physics.R||gy>Physics.H-Physics.R)continue;
    double cx=gx-cue.x,cy=gy-cue.y,approach=Math.hypot(cx,cy);
    if(approach<1e-7)continue;
    double cosine=(cx*dx+cy*dy)/(approach*length);
    if(cosine<.35||!clear(balls,cue.x,cue.y,gx,gy,0,ball.id)||!clear(balls,ball.x,ball.y,p[0],p[1],0,ball.id))continue;
    double distance=approach+length/(cosine*cosine);
    double power=Math.max(Physics.powerForSpeed(.7),Math.min(.8,Physics.powerForSpeed(Math.sqrt(.46*distance+.25))));
    double score=approach*.6+length+(1-cosine)*2;
    // Professional prefers less cut and controlled pace; medium varies among viable pots.
    if(level.equals("medium"))score+=random.nextDouble()*.65;
    if(level.equals("pro"))score+=power*.25;
    candidates.add(new Plan(Math.atan2(cy,cx),power,pocket,ball.id,score));
   }
  }
  candidates.sort(Comparator.comparingDouble(Plan::score));
  if(!candidates.isEmpty()){
   Plan best=candidates.get(0);
   double power=best.power()+(level.equals("beginner")?.09:level.equals("medium")?.02:0);
   return new Plan(best.angle()+noise(level,random),Math.min(.95,power),best.pocket(),best.target(),best.score());
  }
  // No direct pot: a legal, unobstructed first contact with enough pace to reach a rail.
  Physics.Ball target=balls.stream().filter(b->legal(b,balls,group,cleared,false,ruleSet))
   .min(Comparator.comparingDouble(b->Math.hypot(b.x-cue.x,b.y-cue.y)+(clear(balls,cue.x,cue.y,b.x,b.y,0,b.id)?0:20))).orElse(balls.get(8));
  double angle=Math.atan2(target.y-cue.y,target.x-cue.x);
  int pocket=0;double shortest=Double.MAX_VALUE;
  for(int i=0;i<6;i++){double d=Math.hypot(Physics.POCKETS[i][0]-target.x,Physics.POCKETS[i][1]-target.y);if(d<shortest){shortest=d;pocket=i;}}
  return new Plan(angle+noise(level,random),level.equals("beginner")?.55:.40,pocket,target.id,100);
 }
 static double noise(String level,Random random){return (random.nextDouble()*2-1)*(level.equals("beginner")?.055:level.equals("medium")?.012:.0015);}
 static double[] placement(List<Physics.Ball> balls,int group,boolean breaking){return placement(balls,group,breaking,"eight");}
 static double[] placement(List<Physics.Ball> balls,int group,boolean breaking,String ruleSet,String level){
  return level.equals("expert")?ExpertBot.placement(balls,group,breaking,ruleSet):placement(balls,group,breaking,ruleSet);
 }
 static double[] placement(List<Physics.Ball> balls,int group,boolean breaking,String ruleSet){
  boolean cleared=group!=0&&Rules.cleared(balls,group);
  for(Physics.Ball ball:balls){if(!legal(ball,balls,group,cleared,breaking,ruleSet))continue;
   for(double[] pocket:Physics.POCKETS){double dx=pocket[0]-ball.x,dy=pocket[1]-ball.y,d=Math.hypot(dx,dy);if(d<.001)continue;
    double x=ball.x-dx/d*.28,y=ball.y-dy/d*.28;
    if(Physics.canPlace(balls,x,y)&&clear(balls,x,y,ball.x,ball.y,0,ball.id)&&clear(balls,ball.x,ball.y,pocket[0],pocket[1],0,ball.id))return new double[]{x,y};
   }
  }
  for(int row=1;row<=11;row++)for(int col=1;col<=23;col++){
   double x=col*Physics.W/24,y=row*Physics.H/12;if(Physics.canPlace(balls,x,y))return new double[]{x,y};
  }
  throw new IllegalStateException("No valid cue placement");
 }
 static boolean clear(List<Physics.Ball> balls,double x,double y,double tx,double ty,int exclude1,int exclude2){
  double dx=tx-x,dy=ty-y,len2=dx*dx+dy*dy;
  if(len2<1e-12)return true;
  for(Physics.Ball b:balls){if(b.pocketed||b.id==exclude1||b.id==exclude2)continue;
   double t=Math.max(0,Math.min(1,((b.x-x)*dx+(b.y-y)*dy)/len2));
   if(Math.hypot(b.x-(x+t*dx),b.y-(y+t*dy))<CLEARANCE)return false;
  }
  return true;
 }
}
