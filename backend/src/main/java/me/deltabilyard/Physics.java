package me.deltabilyard;
import java.util.*;

/** SI units; playable bed 2.54 x 1.27 m. Deterministic fixed-step simulation. */
public final class Physics {
  public static final double W=2.54,H=1.27,R=.028575,DT=1.0/240;
  public static final double[][] POCKETS={{-.04,-.04},{W/2,-.045},{W+.04,-.04},{-.04,H+.04},{W/2,H+.045},{W+.04,H+.04}};
  public static final double[][] CUSHIONS=makeCushions();
  private static double[][] makeCushions(){
    List<double[]> v=new ArrayList<>();
    for(double y:new double[]{0,H}){double sign=y==0?-1:1;
      v.add(new double[]{.078,y,W/2-.068,y});v.add(new double[]{W/2+.068,y,W-.078,y});
      v.add(new double[]{W/2-.068,y,W/2-.053,y+sign*.10});v.add(new double[]{W/2+.068,y,W/2+.053,y+sign*.10});
    }
    for(double x:new double[]{0,W})v.add(new double[]{x,.078,x,H-.078});
    for(double x:new double[]{0,W})for(double y:new double[]{0,H}){double sx=x==0?-1:1,sy=y==0?-1:1;
      v.add(new double[]{x-sx*.078,y,x-sx*.005,y+sy*.077});
      v.add(new double[]{x,y-sy*.078,x+sx*.077,y-sy*.005});
    }
    return v.toArray(double[][]::new);
  }
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
        for(int p=0;p<6;p++){double[] pt=POCKETS[p];double radius=(p==1||p==4)?.044:.045;
          if((a.x-pt[0])*(a.x-pt[0])+(a.y-pt[1])*(a.y-pt[1])<radius*radius){a.pocketed=true;a.vx=a.vy=0;events.add(new Event(t,"pocket",a.id,-1,p,speed));break;}}
        if(a.pocketed)continue;
        boolean hit=false;
        for(double[] segment:CUSHIONS){double ex=segment[2]-segment[0],ey=segment[3]-segment[1];double u=Math.max(0,Math.min(1,((a.x-segment[0])*ex+(a.y-segment[1])*ey)/(ex*ex+ey*ey)));double px=segment[0]+u*ex,py=segment[1]+u*ey;double dx=a.x-px,dy=a.y-py,d2=dx*dx+dy*dy;if(d2<R*R&&d2>1e-18){double d=Math.sqrt(d2),nx=dx/d,ny=dy/d,vn=a.vx*nx+a.vy*ny;if(vn<0){a.vx-=1.82*vn*nx;a.vy-=1.82*vn*ny;a.vx+=-ny*a.sx*.13;a.vy+=nx*a.sx*.13;hit=true;}a.x=px+nx*R;a.y=py+ny*R;}}
        if(hit){if(contact>=0)rail=true;events.add(new Event(t,"rail",a.id,-1,-1,speed));a.sx*=.6;}
      }
      for(int i=0;i<b.size();i++)for(int j=i+1;j<b.size();j++){
        Ball a=b.get(i),c=b.get(j);if(a.pocketed||c.pocketed)continue;double dx=c.x-a.x,dy=c.y-a.y,d2=dx*dx+dy*dy;
        if(d2>=4*R*R||d2<1e-18)continue;double d=Math.sqrt(d2);double nx=dx/d,ny=dy/d,overlap=2*R-d;
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
  static Frame frame(double t,List<Ball>b){return new Frame(t,b.stream().map(a->new double[]{Math.rint(a.x*10000)/10000,Math.rint(a.y*10000)/10000,a.pocketed?1:0}).toArray(double[][]::new));}
  public static boolean canPlace(List<Ball>b,double x,double y){return Double.isFinite(x)&&Double.isFinite(y)&&x>=R&&x<=W-R&&y>=R&&y<=H-R&&b.stream().noneMatch(a->a.id!=0&&!a.pocketed&&Math.hypot(a.x-x,a.y-y)<R*2.01)&&Arrays.stream(POCKETS).noneMatch(p->Math.hypot(p[0]-x,p[1]-y)<.075);}
}
