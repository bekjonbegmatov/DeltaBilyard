import type {BotLevel} from './types';

export const botLevels:readonly BotLevel[]=['beginner','medium','pro','expert'];
export const botLevelNames:Record<BotLevel,string>={beginner:'Новичок',medium:'Средний',pro:'Профи',expert:'Эксперт'};
export const botLevelDescriptions:Record<BotLevel,string>={
 beginner:'Спокойная игра для освоения ударов',
 medium:'Уверенные удары, но ещё бывают ошибки',
 pro:'Точный соперник для опытных игроков',
 expert:'Просчитывает удары и положение битка',
};
