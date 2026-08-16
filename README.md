# HezhongSkywars
A Simple open-source minecraft skywars plugin, supports mc 1.8 ~ latest.

## Bungee架构规划
1、数据库同步，游戏服务器必须等待玩家战绩保存后，才可以发送至其它服务器。保留当前单端的数据库存储和保护系统，再额外新增一套跨服的。  
2、大厅服务器定时向代理报告自己，代理服务器记录  
3、游戏服务器发生状态变化时向代理端报告，代理端给所有大厅服务器报告，大厅服务器正常显示所有游戏
