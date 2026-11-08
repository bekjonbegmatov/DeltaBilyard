package me.deltabilyard;
import java.util.*;

/** SI units; playable bed 2.54 x 1.27 m. Deterministic fixed-step simulation. */
public final class Physics {
  public static final double W=2.54,H=1.27,R=.028575,DT=1.0/240;
  public static final double[][] POCKETS={{0,0},{W/2,0},{W,0},{0,H},{W/2,H},{W,H}};
  public static class Ball {
    public int id; public double x,y,vx,vy,sx,sy; public boolean pocketed;
    public Ball() {} public Ball(int id,double x,double y){this.id=id;this.x=x;this.y=y;}
    public Ball copy(){Ball b=new Ball(id,x,y); b.vx=vx;b.vy=vy;b.sx=sx;b.sy=sy;b.pocketed=pocketed;return b;}
  }
  public record Event(double t,String type,int ball,int other,int pocket,double strength){}
  public record Frame(double t,double[][] balls){}
  public record Shot(List<Ball> balls,List<Frame> frames,List<Event> events,int firstContact,boolean railAfterContact,double duration){}
  public static List<Ball> rack(){
    List<Ball> balls=new ArrayList<>();balls.add(new Ball(0,W*.25,H/2));
    int[] ids={1,9,2,10,8,3,4,11,5,12,6,13,7,14,15};int k=0;
    for(int row=0;row<5;row++)for(int col=0;col<=row;col++)balls.add(new Ball(ids[k++],W*.72+row*R*Math.sqrt(3)*1.005,H/2+(2*col-row)*R*1.005));
    balls.sort(Comparator.comparingInt(b->b.id));return balls;
  }
  public static Shot simulate(List<Ball> initial,double angle,double power,double spinX,double spinY){
    if(!Double.isFinite(angle)||!Double.isFinite(power)||!Double.isFinite(spinX)||!Double.isFinite(spinY)||power<.02||power>1||Math.hypot(spinX,spinY)>1.001)throw new IllegalArgumentException("Недопустимый удар");
    List<Ball> b=initial.stream().map(Ball::copy).toList();Ball cue=b.get(0);cue.vx=Math.cos(angle)*power*6;cue.vy=Math.sin(angle)*power*6;cue.sx=spinX;cue.sy=spinY;
    List<Frame> frames=new ArrayList<>();List<Event> events=new ArrayList<>();int contact=-1;boolean rail=false;double t=0;
    frames.add(frame(0,b));
    for(int step=1;step<=240*24;step++){
      t=step*DT;boolean moving=false;
      for(Ball a:b){if(a.pocketed)continue;
        double speed=Math.hypot(a.vx,a.vy);if(speed>.008){moving=true;
          // Top/back spin changes cue velocity during the initial sliding phase.
          if(a.id==0 && t<.35){double accel=a.sy*.65; a.vx+=Math.cos(angle)*accel*DT;a.vy+=Math.sin(angle)*accel*DT;}
          a.x+=a.vx*DT;a.y+=a.vy*DT;
          double factor=Math.max(0,(speed-.23*DT)/speed);a.vx*=factor;a.vy*=factor;
        }else{a.vx=0;a.vy=0;}
        for(int p=0;p<6;p++){double[] pt=POCKETS[p];double radius=(p==1||p==4)?.061:.067;
          if(Math.hypot(a.x-pt[0],a.y-pt[1])<radius){a.pocketed=true;a.vx=a.vy=0;events.add(new Event(t,"pocket",a.id,-1,p,speed));break;}}
        if(a.pocketed)continue;
        boolean hit=false;
        if(a.x<R){a.x=R;a.vx=Math.abs(a.vx)*.82;hit=true;}if(a.x>W-R){a.x=W-R;a.vx=-Math.abs(a.vx)*.82;hit=true;}
        if(a.y<R){a.y=R;a.vy=Math.abs(a.vy)*.82;a.vx+=a.sx*.15;hit=true;}if(a.y>H-R){a.y=H-R;a.vy=-Math.abs(a.vy)*.82;a.vx-=a.sx*.15;hit=true;}
        if(hit){if(contact>=0)rail=true;events.add(new Event(t,"rail",a.id,-1,-1,speed));a.sx*=.6;}
      }
      for(int i=0;i<b.size();i++)for(int j=i+1;j<b.size();j++){
        Ball a=b.get(i),c=b.get(j);if(a.pocketed||c.pocketed)continue;double dx=c.x-a.x,dy=c.y-a.y,d=Math.hypot(dx,dy);
        if(d>=2*R||d<1e-9)continue;double nx=dx/d,ny=dy/d,overlap=2*R-d;
        a.x-=nx*overlap/2;a.y-=ny*overlap/2;c.x+=nx*overlap/2;c.y+=ny*overlap/2;
        double v=(a.vx-c.vx)*nx+(a.vy-c.vy)*ny;if(v<=0)continue;
        if(contact<0&&(a.id==0||c.id==0))contact=a.id==0?c.id:a.id;
        double impulse=v*.98;a.vx-=impulse*nx;a.vy-=impulse*ny;c.vx+=impulse*nx;c.vy+=impulse*ny;
        if(a.id==0){a.vx+=Math.cos(angle)*a.sy*.18;a.vy+=Math.sin(angle)*a.sy*.18;a.sy=0;}
        events.add(new Event(t,"collision",a.id,c.id,-1,v));
      }
      if(step%8==0)frames.add(frame(t,b));
      if(!moving&&step>2)break;
    }
    for(Ball a:b){a.vx=a.vy=a.sx=a.sy=0;}frames.add(frame(t,b));
    return new Shot(b,frames,events,contact,rail,t);
  }
  static Frame frame(double t,List<Ball>b){return new Frame(t,b.stream().map(a->new double[]{a.x,a.y,a.pocketed?1:0}).toArray(double[][]::new));}
  public static boolean canPlace(List<Ball>b,double x,double y){return Double.isFinite(x)&&Double.isFinite(y)&&x>=R&&x<=W-R&&y>=R&&y<=H-R&&b.stream().noneMatch(a->a.id!=0&&!a.pocketed&&Math.hypot(a.x-x,a.y-y)<R*2.01)&&Arrays.stream(POCKETS).noneMatch(p->Math.hypot(p[0]-x,p[1]-y)<.075);}
}
