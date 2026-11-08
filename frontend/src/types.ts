export type User={id:number;name:string;username?:string;avatar?:string};
export type Ball={id:number;x:number;y:number;pocketed:boolean};
export type Shot={duration:number;frames:{t:number;balls:number[][]}[];events:{t:number;type:string;ball:number;pocket:number;strength:number}[]};
export type Room={code:string;id:string;phase:string;mode:string;seconds:number;target:number;hints:boolean;seats:{user:User;ready:boolean}[];balls:Ball[];turn:number;seq:number;round:number;winner:number;message:string;groups:number[];scores:number[];ballInHand:boolean;breaking:boolean;deadline:number;shotAt:number;shot?:Shot;disconnected:Record<string,number>};
export type Tg={initData:string;initDataUnsafe?:{start_param?:string};ready:()=>void;expand:()=>void;requestFullscreen?:()=>void;lockOrientation?:()=>void;unlockOrientation?:()=>void;disableVerticalSwipes?:()=>void;isVersionAtLeast?:(v:string)=>boolean;setHeaderColor?:(c:string)=>void;setBackgroundColor?:(c:string)=>void;openTelegramLink?:(url:string)=>void;contentSafeAreaInset?:{top:number;bottom:number;left:number;right:number}};
declare global{interface Window{Telegram?:{WebApp:Tg}}}
