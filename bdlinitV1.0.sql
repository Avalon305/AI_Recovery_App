/*
Navicat MySQL Data Transfer

Source Server         : localhostMysql8.0
Source Server Version : 80016
Source Host           : 127.0.0.1:3306
Source Database       : bdlinit

Target Server Type    : MYSQL
Target Server Version : 80016
File Encoding         : 65001

Date: 2019-08-11 16:46:36
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for bdl_auth
-- ----------------------------
DROP TABLE IF EXISTS `bdl_auth`;
CREATE TABLE `bdl_auth` (
  `pk_auth_id` int(4) NOT NULL AUTO_INCREMENT COMMENT '权限表id',
  `auth_username` varchar(255) DEFAULT NULL COMMENT '权限用户名',
  `auth_userpass` varchar(255) DEFAULT NULL COMMENT '密码',
  `auth_level` tinyint(1) DEFAULT NULL COMMENT '角色权限级别',
  `gmt_create` timestamp NULL DEFAULT NULL,
  `gmt_modified` timestamp NULL DEFAULT NULL,
  `user_status` int(3) DEFAULT NULL COMMENT '使用状态',
  `auth_offlinetime` date DEFAULT NULL COMMENT '离线时间',
  PRIMARY KEY (`pk_auth_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_auth
-- ----------------------------
INSERT INTO `bdl_auth` VALUES ('1', 'admin', 'admin', '0', '2018-04-07 15:44:04', '2018-04-07 15:44:08', '2', '9999-12-31');
INSERT INTO `bdl_auth` VALUES ('4', '123', '123', '1', '2019-07-05 17:58:34', '2019-07-05 17:58:34', '2', '9999-12-31');

-- ----------------------------
-- Table structure for bdl_customdata
-- ----------------------------
DROP TABLE IF EXISTS `bdl_customdata`;
CREATE TABLE `bdl_customdata` (
  `pk_cd_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cd_customname` varchar(255) NOT NULL COMMENT '自定义姓名',
  `cd_type` tinyint(255) NOT NULL COMMENT '类型编号，枚举控制',
  `is_deleted` tinyint(4) NOT NULL COMMENT '是否删除  默认0不删除',
  PRIMARY KEY (`pk_cd_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_customdata
-- ----------------------------

-- ----------------------------
-- Table structure for bdl_datacode
-- ----------------------------
DROP TABLE IF EXISTS `bdl_datacode`;
CREATE TABLE `bdl_datacode` (
  `fk_code_id` int(11) NOT NULL AUTO_INCREMENT,
  `code_xh` int(11) DEFAULT NULL COMMENT '排序号，下拉列表按这个排序',
  `code_type_id` varchar(255) DEFAULT NULL COMMENT '类型ID，dList是数据项',
  `code_s_value` varchar(255) DEFAULT NULL COMMENT '存储值',
  `code_c_value` varchar(255) DEFAULT NULL COMMENT '展示值',
  `code_state` tinyint(4) DEFAULT NULL COMMENT '是否启用 0 不启用 1启用',
  `code_e_value` varchar(255) DEFAULT NULL COMMENT '英语显示值',
  PRIMARY KEY (`fk_code_id`)
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_datacode
-- ----------------------------
INSERT INTO `bdl_datacode` VALUES ('1', '1', 'DList', 'Evaluate', '时机，姿势，评价', '1', 'Evaluate');
INSERT INTO `bdl_datacode` VALUES ('2', '1', 'Evaluate', '0', '没有问题', '1', 'Normal');
INSERT INTO `bdl_datacode` VALUES ('3', '2', 'Evaluate', '1', '有些许问题', '1', 'Slightly Not Normal');
INSERT INTO `bdl_datacode` VALUES ('4', '3', 'Evaluate', '2', '有问题', '1', 'Not Normal');
INSERT INTO `bdl_datacode` VALUES ('14', '1', 'DList', 'MoveWay', '移乘方式', '1', 'MoveWay');
INSERT INTO `bdl_datacode` VALUES ('15', '1', 'MoveWay', '0', '自理', '1', 'Independent');
INSERT INTO `bdl_datacode` VALUES ('16', '2', 'MoveWay', '1', '照看', '1', 'Need Observing');
INSERT INTO `bdl_datacode` VALUES ('17', '3', 'MoveWay', '2', '完全失能', '1', 'Full Supported');
INSERT INTO `bdl_datacode` VALUES ('20', '1', 'DList', 'OrganizationSort', '机构类别', '1', 'OrganizationSort');
INSERT INTO `bdl_datacode` VALUES ('21', '1', 'OrganizationSort', '0', '医院', '1', 'Hospital');
INSERT INTO `bdl_datacode` VALUES ('22', '2', 'OrganizationSort', '1', '诊所', '1', 'Clinic');
INSERT INTO `bdl_datacode` VALUES ('23', '3', 'OrganizationSort', '2', '老人保健机构', '1', 'Health care institution');
INSERT INTO `bdl_datacode` VALUES ('24', '4', 'OrganizationSort', '3', '特别护理老人院', '1', 'Special nursing home');
INSERT INTO `bdl_datacode` VALUES ('25', '5', 'OrganizationSort', '4', '日托服务', '1', 'Daycare facilities');
INSERT INTO `bdl_datacode` VALUES ('26', '6', 'OrganizationSort', '5', '市区政府', '1', 'Urban government');
INSERT INTO `bdl_datacode` VALUES ('27', '7', 'OrganizationSort', '6', '私立老人院', '1', 'Private seniors');
INSERT INTO `bdl_datacode` VALUES ('28', '8', 'OrganizationSort', '7', '其他', '1', 'Other');
INSERT INTO `bdl_datacode` VALUES ('30', '1', 'DList', 'Language', '语言', '1', 'Language');
INSERT INTO `bdl_datacode` VALUES ('31', '1', 'Language', '0', 'English', '1', 'English');
INSERT INTO `bdl_datacode` VALUES ('32', '2', 'Language', '1', '中文', '1', '中文');
INSERT INTO `bdl_datacode` VALUES ('33', '1', 'DList', 'CareLevel', '要介护度', '1', 'CareLevel');
INSERT INTO `bdl_datacode` VALUES ('34', '1', 'CareLevel', '0', '没有申请', '1', 'No application');
INSERT INTO `bdl_datacode` VALUES ('35', '2', 'CareLevel', '1', '自理', '1', 'Self-care');
INSERT INTO `bdl_datacode` VALUES ('36', '3', 'CareLevel', '2', '要支援一', '1', 'To support one');
INSERT INTO `bdl_datacode` VALUES ('37', '4', 'CareLevel', '3', '要支援二', '1', 'To support two');
INSERT INTO `bdl_datacode` VALUES ('38', '5', 'CareLevel', '4', '要护理1', '1', 'To care 1');
INSERT INTO `bdl_datacode` VALUES ('39', '6', 'CareLevel', '5', '要护理2', '1', 'To care 2');
INSERT INTO `bdl_datacode` VALUES ('40', '7', 'CareLevel', '6', '要护理3', '1', 'To care 3');
INSERT INTO `bdl_datacode` VALUES ('41', '8', 'CareLevel', '7', '要护理4', '1', 'To care 4');
INSERT INTO `bdl_datacode` VALUES ('42', '9', 'CareLevel', '8', '要护理5', '1', 'To care 5');
INSERT INTO `bdl_datacode` VALUES ('43', null, 'DLIST', 'TRAIN_MODE', '训练模式', null, null);
INSERT INTO `bdl_datacode` VALUES ('44', '1', 'TRAIN_MODE', '0', '主动模式', null, null);
INSERT INTO `bdl_datacode` VALUES ('45', '2', 'TRAIN_MODE', '1', '被动模式', null, null);
INSERT INTO `bdl_datacode` VALUES ('46', '3', 'TRAIN_MODE', '2', '康复模式', null, null);

-- ----------------------------
-- Table structure for bdl_deviceprescription
-- ----------------------------
DROP TABLE IF EXISTS `bdl_deviceprescription`;
CREATE TABLE `bdl_deviceprescription` (
  `pk_dp_id` int(8) NOT NULL AUTO_INCREMENT COMMENT '设备处方id',
  `gmt_create` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `fk_ti_id` int(8) DEFAULT NULL COMMENT '外键（bdl_trianinfo : pk_ti_id）',
  `fk_ds_id` int(8) DEFAULT NULL COMMENT '外键（bdl_devicesort : pk_ds_id）',
  `sport_mode` int(11) DEFAULT NULL COMMENT '用户运动模式:0：计数模式，1：计时模式',
  `device_mode` int(11) DEFAULT NULL COMMENT '设备训练模式:0康复模式，1主被动模式,2被动模式',
  `dp_status` tinyint(1) DEFAULT NULL COMMENT '1做了 0没做',
  `dp_moveway` int(3) DEFAULT NULL COMMENT '移乘方式',
  `dp_memo` text COMMENT '注意点、指示',
  `dp_timecount` int(4) DEFAULT NULL COMMENT '目标运动时间',
  `dp_target_num` int(11) DEFAULT NULL COMMENT '目标运动个数',
  `speed_rank` int(11) DEFAULT NULL COMMENT '运动速度等级',
  `seat_height` int(10) unsigned DEFAULT NULL COMMENT '座位高度cm',
  `backrest_distance` int(10) unsigned DEFAULT NULL COMMENT '靠背距离',
  `footboard_distance` int(10) unsigned DEFAULT NULL COMMENT '踏板距离',
  `lever_angle` double(10,2) DEFAULT NULL COMMENT '杠杆角度',
  `front_limit` int(10) DEFAULT NULL COMMENT '前方限制',
  `back_limit` int(10) DEFAULT NULL COMMENT '后方限制',
  `consequent_force` double(10,2) unsigned DEFAULT NULL COMMENT '顺向力',
  `reverse_force` double(10,2) DEFAULT NULL COMMENT '反向力',
  `power` double(10,2) DEFAULT NULL COMMENT '功率',
  `extra_setting` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '额外属性：为保证设备属性的可扩展性，存储为Json串Key、value',
  PRIMARY KEY (`pk_dp_id`)
) ENGINE=InnoDB AUTO_INCREMENT=179 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_deviceprescription
-- ----------------------------

-- ----------------------------
-- Table structure for bdl_deviceset
-- ----------------------------
DROP TABLE IF EXISTS `bdl_deviceset`;
CREATE TABLE `bdl_deviceset` (
  `bdl_dset_id` int(8) NOT NULL AUTO_INCREMENT COMMENT '设备系列id',
  `gmt_create` timestamp NULL DEFAULT NULL,
  `gmt_modified` timestamp NULL DEFAULT NULL,
  `dset_name` varchar(255) DEFAULT NULL COMMENT '设备系列名',
  PRIMARY KEY (`bdl_dset_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_deviceset
-- ----------------------------
INSERT INTO `bdl_deviceset` VALUES ('1', '2018-04-19 21:47:16', '2018-04-19 21:47:18', '宝德龙系列');

-- ----------------------------
-- Table structure for bdl_devicesort
-- ----------------------------
DROP TABLE IF EXISTS `bdl_devicesort`;
CREATE TABLE `bdl_devicesort` (
  `pk_ds_id` int(8) NOT NULL AUTO_INCREMENT COMMENT '设备分类id',
  `ds_name` varchar(255) DEFAULT NULL COMMENT '分类名称',
  `gmt_create` timestamp NULL DEFAULT NULL,
  `gmt_modified` timestamp NULL DEFAULT NULL,
  `fk_dset_id` int(8) DEFAULT NULL COMMENT '外键（bdl_deviceset : pk_dset_id）',
  `ds_status` tinyint(1) DEFAULT NULL COMMENT '复选框状态是否选中',
  PRIMARY KEY (`pk_ds_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_devicesort
-- ----------------------------
INSERT INTO `bdl_devicesort` VALUES ('2', '腿部外弯机', '2018-04-19 21:47:40', '2018-04-19 21:47:44', '1', '1');
INSERT INTO `bdl_devicesort` VALUES ('5', '坐式划船机', '2018-03-21 23:54:19', '2018-03-21 23:54:21', '1', '1');
INSERT INTO `bdl_devicesort` VALUES ('6', '腿部推蹬机', '2018-04-19 21:47:36', '2018-04-19 21:47:38', '1', '1');
INSERT INTO `bdl_devicesort` VALUES ('7', '坐式推胸机', null, null, '1', '1');
INSERT INTO `bdl_devicesort` VALUES ('8', '腹肌训练机', null, null, '1', '1');
INSERT INTO `bdl_devicesort` VALUES ('9', '三头肌训练机', null, null, '1', '1');
INSERT INTO `bdl_devicesort` VALUES ('10', '腿部内弯机', null, null, '1', '1');
INSERT INTO `bdl_devicesort` VALUES ('11', '蝴蝶机', null, null, '1', '1');
INSERT INTO `bdl_devicesort` VALUES ('12', '反向蝴蝶机', null, null, '1', '1');
INSERT INTO `bdl_devicesort` VALUES ('13', '坐式背部伸展机', null, null, '1', '1');

-- ----------------------------
-- Table structure for bdl_onlinedevice
-- ----------------------------
DROP TABLE IF EXISTS `bdl_onlinedevice`;
CREATE TABLE `bdl_onlinedevice` (
  `pk_od_id` int(11) NOT NULL AUTO_INCREMENT,
  `od_clientid` varchar(255) DEFAULT NULL,
  `od_clientname_en` varchar(255) DEFAULT NULL,
  `od_clientname_ch` varchar(255) DEFAULT NULL,
  `od_gmt_modified` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`pk_od_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_onlinedevice
-- ----------------------------

-- ----------------------------
-- Table structure for bdl_personal_setting
-- ----------------------------
DROP TABLE IF EXISTS `bdl_personal_setting`;
CREATE TABLE `bdl_personal_setting` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键自增id',
  `fk_member_id` int(10) DEFAULT NULL COMMENT '关联bdl_user表的主键',
  `member_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '会员卡号ID',
  `device_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '设备名',
  `device_order_number` int(10) unsigned DEFAULT NULL COMMENT '设备序号',
  `training_mode` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '训练模式',
  `seat_height` int(10) unsigned DEFAULT NULL COMMENT '座位高度cm',
  `backrest_distance` int(10) unsigned DEFAULT NULL COMMENT '靠背距离',
  `footboard_distance` int(10) unsigned DEFAULT NULL COMMENT '踏板距离',
  `lever_angle` double(10,2) DEFAULT NULL COMMENT '杠杆角度',
  `front_limit` int(10) DEFAULT NULL COMMENT '前方限制',
  `back_limit` int(10) DEFAULT NULL COMMENT '后方限制',
  `consequent_force` double(10,2) unsigned DEFAULT NULL COMMENT '顺向力',
  `reverse_force` double(10,2) DEFAULT NULL COMMENT '反向力',
  `power` double(10,2) DEFAULT NULL COMMENT '功率',
  `extra_setting` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '额外属性：为保证设备属性的可扩展性，存储为Json串Key、value',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `member_id` (`member_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='bdl_personal_setting个人设置';

-- ----------------------------
-- Records of bdl_personal_setting
-- ----------------------------
INSERT INTO `bdl_personal_setting` VALUES ('1', null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, '2019-08-10 19:03:19', null);

-- ----------------------------
-- Table structure for bdl_physicalpower
-- ----------------------------
DROP TABLE IF EXISTS `bdl_physicalpower`;
CREATE TABLE `bdl_physicalpower` (
  `pk_pp_id` int(8) NOT NULL AUTO_INCREMENT COMMENT '体力评价id',
  `gmt_create` timestamp NULL DEFAULT NULL,
  `gmt_modified` timestamp NULL DEFAULT NULL,
  `fk_user_id` int(8) DEFAULT NULL COMMENT '外键（bdl_user : pk_user_id）',
  `pp_high` varchar(255) DEFAULT NULL COMMENT '身高',
  `pp_weight` varchar(255) DEFAULT NULL COMMENT '体重',
  `pp_grip` varchar(255) DEFAULT NULL COMMENT '握力',
  `pp_eyeopenstand` varchar(255) DEFAULT NULL COMMENT '睁眼单脚站立',
  `pp_functionprotract` varchar(255) DEFAULT NULL COMMENT '功能性前伸',
  `pp_sitandreach` varchar(255) DEFAULT NULL COMMENT '坐姿体前屈',
  `pp_timeupgo` varchar(255) DEFAULT NULL COMMENT 'timeup go',
  `pp_walk5milegeneral` varchar(255) DEFAULT NULL COMMENT '5m步行',
  `pp_walk5milefast` varchar(255) DEFAULT NULL COMMENT '5m步行，最快',
  `pp_walk10mile` varchar(255) DEFAULT NULL COMMENT '10m步行，第一个字段是模式',
  `pp_walk6minute` varchar(255) DEFAULT NULL COMMENT '6分钟步行',
  `pp_step2minute` varchar(255) DEFAULT NULL COMMENT '2分钟踏步',
  `pp_legraise2minute` varchar(255) DEFAULT NULL COMMENT '2分钟抬腿',
  `pp_usermemo` text COMMENT '利用者感想',
  `pp_workermemo` text COMMENT '工作人员感想',
  PRIMARY KEY (`pk_pp_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_physicalpower
-- ----------------------------

-- ----------------------------
-- Table structure for bdl_prescriptionresult
-- ----------------------------
DROP TABLE IF EXISTS `bdl_prescriptionresult`;
CREATE TABLE `bdl_prescriptionresult` (
  `pk_pr_id` int(8) NOT NULL AUTO_INCREMENT COMMENT '处方结果id',
  `fk_dp_id` int(8) DEFAULT NULL COMMENT '外键（dbl_deviceprescreiption : pk_dp_id）',
  `fk_ds_id` int(11) DEFAULT NULL COMMENT '关联的单一设备类型ID',
  `bind_id` varchar(255) DEFAULT NULL COMMENT '用户手环ID',
  `sport_mode` int(10) unsigned DEFAULT NULL COMMENT '用户运动模式,0：计数模式，1：计时模式',
  `device_mode` int(10) unsigned DEFAULT NULL COMMENT '设备训练模式:训练模式：0康复模式，1主被动模式,2被动模式',
  `consequent_force` double(10,2) unsigned DEFAULT NULL COMMENT '顺向力',
  `reverse_force` double(10,2) DEFAULT NULL COMMENT '反向力',
  `power` double(10,2) DEFAULT NULL COMMENT '功率',
  `speed_rank` int(3) DEFAULT NULL COMMENT '运动速度等级:主被动模式/被动模式 只能选运动速度',
  `finishnum` int(10) unsigned DEFAULT NULL COMMENT '完成运动个数：计数模式完成个数',
  `finish_time` int(11) DEFAULT NULL COMMENT '完成运动时间：计时模式运动时间',
  `distance` double(10,2) unsigned DEFAULT NULL COMMENT '距离 千米，两位小数',
  `energy` double(10,2) DEFAULT NULL COMMENT '训练总耗能 单位卡路里',
  `heart_rate_list` text COMMENT '心率集合：运动过程实时心率集合，数据之间*分割',
  `gmt_create` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`pk_pr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_prescriptionresult
-- ----------------------------

-- ----------------------------
-- Table structure for bdl_set
-- ----------------------------
DROP TABLE IF EXISTS `bdl_set`;
CREATE TABLE `bdl_set` (
  `pk_set_id` int(8) NOT NULL AUTO_INCREMENT COMMENT '设置id',
  `set_unique_id` text COMMENT '客户机唯一id',
  `set_organizationname` varchar(255) DEFAULT NULL COMMENT '组织名称',
  `set_photolocation` varchar(255) DEFAULT NULL COMMENT '照片位置',
  `set_organizationphone` varchar(255) DEFAULT NULL COMMENT '联系电话',
  `set_language` varchar(255) DEFAULT NULL,
  `set_organizationsort` varchar(255) DEFAULT NULL,
  `set_version` varchar(255) DEFAULT NULL,
  `back_up` varchar(255) DEFAULT NULL COMMENT '备份路径',
  PRIMARY KEY (`pk_set_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_set
-- ----------------------------
INSERT INTO `bdl_set` VALUES ('5', '2DE22181A14ADBD0BB1D437550B1A0F61AD1AE0A59B79794CBCD3C1607AEBD48', 'bodystrong03', 'D:\\SPMS_ALL\\spms2019-7-5\\image\\', '', '1', '7', '1.0', 'D:\\SPMS_ALL\\spms2019-7-5\\BackUp\\');

-- ----------------------------
-- Table structure for bdl_skeleton_length
-- ----------------------------
DROP TABLE IF EXISTS `bdl_skeleton_length`;
CREATE TABLE `bdl_skeleton_length` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `fk_member_id` int(11) DEFAULT NULL COMMENT '关联bdl_user的主键',
  `body_length` double DEFAULT NULL COMMENT '躯干长度(脖子到屁股~)',
  `shoulder_width` double DEFAULT NULL COMMENT '肩宽(单侧肩宽)',
  `arm_length_up` double DEFAULT NULL COMMENT '臂长(上部分)',
  `arm_length_down` double DEFAULT NULL COMMENT '臂长(下部分)',
  `leg_length_up` double DEFAULT NULL COMMENT '腿长(上部分)',
  `leg_length_down` double DEFAULT NULL COMMENT '腿长(下部分)',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of bdl_skeleton_length
-- ----------------------------
INSERT INTO `bdl_skeleton_length` VALUES ('1', '801', '47.12', '18.4', '29.44', '26.77', '43.19', '42.51', '2019-08-10 16:31:24', null);

-- ----------------------------
-- Table structure for bdl_symptominfo
-- ----------------------------
DROP TABLE IF EXISTS `bdl_symptominfo`;
CREATE TABLE `bdl_symptominfo` (
  `pk_si_id` int(8) NOT NULL AUTO_INCREMENT COMMENT '症状id',
  `gmt_create` timestamp NULL DEFAULT NULL COMMENT '创建日期',
  `gmt_modified` timestamp NULL DEFAULT NULL COMMENT '修改日期',
  `fk_user_id` int(8) DEFAULT NULL COMMENT '外键（dbl_user : pk_user_id）',
  `fk_ti_id` int(8) DEFAULT NULL COMMENT '外键（dbl_traininfo : pk_ti_id）',
  `si_isjoin` tinyint(1) DEFAULT NULL COMMENT '是否参加',
  `si_waterinput` varchar(255) DEFAULT NULL COMMENT '水分摄取',
  `si_careinfo` text COMMENT '看护记录',
  `si_inquiry` varchar(255) DEFAULT NULL COMMENT '问诊票',
  `si_pre_highpressure` varchar(255) DEFAULT NULL COMMENT '高血压(康复前)',
  `si_pre_lowpressure` varchar(255) DEFAULT NULL COMMENT '低血压(康复前)',
  `si_pre_heartrate` varchar(255) DEFAULT NULL COMMENT '心率(康复前)',
  `si_pre_pulse` int(3) DEFAULT NULL COMMENT '脉(康复前)',
  `si_pre_animalheat` varchar(255) DEFAULT NULL COMMENT '体温(康复前)',
  `si_suf_highpressure` varchar(255) DEFAULT NULL COMMENT '高血压(康复后)',
  `si_suf_lowpressure` varchar(255) DEFAULT NULL COMMENT '低血压(康复后)',
  `si_suf_heartrate` varchar(255) DEFAULT NULL COMMENT '心率(康复后)',
  `si_suf_pulse` int(3) DEFAULT NULL COMMENT '脉(康复后)',
  `si_suf_animalheat` varchar(255) DEFAULT NULL COMMENT '体温(康复后)',
  PRIMARY KEY (`pk_si_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_symptominfo
-- ----------------------------

-- ----------------------------
-- Table structure for bdl_traininfo
-- ----------------------------
DROP TABLE IF EXISTS `bdl_traininfo`;
CREATE TABLE `bdl_traininfo` (
  `pk_ti_id` int(8) NOT NULL AUTO_INCREMENT COMMENT '训练信息id',
  `gmt_create` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `fk_user_id` int(8) DEFAULT NULL COMMENT '外键（dbl_user : pk_user_id）',
  `status` int(1) DEFAULT NULL COMMENT '0 未做 1 完成 2 废弃',
  PRIMARY KEY (`pk_ti_id`)
) ENGINE=InnoDB AUTO_INCREMENT=138 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_traininfo
-- ----------------------------

-- ----------------------------
-- Table structure for bdl_uploadmanagement
-- ----------------------------
DROP TABLE IF EXISTS `bdl_uploadmanagement`;
CREATE TABLE `bdl_uploadmanagement` (
  `pk_um_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键 自增',
  `um_dataid` int(11) NOT NULL COMMENT '待上传的数据的id',
  `um_datatable` varchar(255) NOT NULL COMMENT '待上传的数据表的名字',
  `um_exec` int(11) unsigned NOT NULL,
  PRIMARY KEY (`pk_um_id`)
) ENGINE=InnoDB AUTO_INCREMENT=326 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_uploadmanagement
-- ----------------------------
INSERT INTO `bdl_uploadmanagement` VALUES ('324', '0', 'bdl_set', '0');
INSERT INTO `bdl_uploadmanagement` VALUES ('325', '5', 'bdl_set', '1');

-- ----------------------------
-- Table structure for bdl_user
-- ----------------------------
DROP TABLE IF EXISTS `bdl_user`;
CREATE TABLE `bdl_user` (
  `pk_user_id` int(8) NOT NULL AUTO_INCREMENT COMMENT '病人id',
  `gmt_create` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `gmt_modified` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `user_name` varchar(255) DEFAULT NULL COMMENT '病人姓名',
  `user_namepinyin` varchar(255) DEFAULT NULL COMMENT '姓名拼音',
  `user_sex` tinyint(1) DEFAULT NULL COMMENT '性别',
  `user_birth` date DEFAULT NULL COMMENT '出生日期',
  `user_groupname` varchar(255) DEFAULT NULL COMMENT '小组名称',
  `user_initcare` varchar(255) DEFAULT NULL COMMENT '初期要介护度',
  `user_nowcare` varchar(255) DEFAULT NULL COMMENT '现在要介护度',
  `user_illnessname` varchar(255) DEFAULT NULL COMMENT '疾病名称',
  `user_physicaldisabilities` varchar(255) DEFAULT NULL COMMENT '残障名称',
  `user_memo` text COMMENT '备忘',
  `user_photolocation` varchar(255) DEFAULT NULL COMMENT '照片位置',
  `user_idcard` varchar(255) DEFAULT NULL COMMENT 'id卡',
  `user_phone` varchar(255) DEFAULT NULL COMMENT '电话号码',
  `is_deleted` tinyint(1) DEFAULT NULL COMMENT '是否删除',
  `user_privateinfo` text COMMENT '非公开信息',
  PRIMARY KEY (`pk_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_user
-- ----------------------------

-- ----------------------------
-- Table structure for bdl_user_relation
-- ----------------------------
DROP TABLE IF EXISTS `bdl_user_relation`;
CREATE TABLE `bdl_user_relation` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `fk_user_id` int(11) DEFAULT NULL COMMENT '关联的用户ID',
  `bind_id` varchar(255) DEFAULT '' COMMENT '绑定的手环ID,用户绑定的蓝牙手环名称ID',
  `muscle_test_val` varchar(255) DEFAULT NULL COMMENT '肌力测试值',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of bdl_user_relation
-- ----------------------------
