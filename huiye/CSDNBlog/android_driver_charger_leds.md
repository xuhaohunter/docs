---
title: android driver 调试led三色电源指示灯 
---
最近同事请假了，我帮忙调试了led三色灯，修改的地方如下：
1. kernel/arch/arm64/configs/msm-perf_defconfig
   kernel/arch/arm64/configs/msm_defconfig 
-------------------------------------------
见BoardConfig.mk
```cpp
   CONFIG_LEDS_AW2013=y
```
(2) kernel/arch/arm/boot/dts/msm8952_x42_ktouch_c1/msm8952-qrd-skum.dtsi
kernel/arch/arm/boot/dts/qcom/msm8952-qrd-skum.dtsi
为了防止编译脚本的问题，qcom目录下的也要修改。

去掉mpp相关的第一个节点
```cpp
   &spmi_bus {
        qcom,pmi8950@2 {
		    qcom,leds@a100 {
			    status = "okay";
			    qcom,led_mpp_2 {
			        label = "mpp";
				    linux,name = "green";
				    linux,default-trigger = "none";
				    qcom,default-state = "off";
				    qcom,max-current = <20>;
				    qcom,current-setting = <5>;
				    qcom,id = <6>;
				    qcom,mode = "manual";
				    qcom,source-sel = <1>;
				    qcom,mode-ctrl = <0x60>;
			    };
		    };
	    };

	    qcom,pmi8950@3 {
		    qcom,leds@d800 {
			    qcom,fs-curr-ua = <20000>;
		    };
	    };
    };
```
配置/sys/class/leds/red和/sys/class/leds/green两个节点
```cpp
    i2c@7af5000 { /* BLSP2 QUP1 */
	    aw2013@45 {
			compatible = "awinic,aw2013";
			reg = <0x45>;
			vdd-supply = <&pm8950_l17>;
			vcc-supply = <&pm8950_l6>;
		    aw2013,green {
			    aw2013,name = "green";
			    aw2013,id = <0>;
			    aw2013,max-brightness = <255>;
			    aw2013,max-current = <1>;
				aw2013,rise-time-ms = <2>;
				aw2013,hold-time-ms = <1>;
				aw2013,fall-time-ms = <2>;
				aw2013,off-time-ms = <1>;
			};

		    aw2013,red {
				aw2013,name = "red";
				aw2013,id = <1>;
				aw2013,max-brightness = <255>;
				aw2013,max-current = <1>;
				aw2013,rise-time-ms = <2>;
				aw2013,hold-time-ms = <1>;
				aw2013,fall-time-ms = <2>;
				aw2013,off-time-ms = <1>;
			};
		};
	};
```
去掉qcom,chg-led-support
```cpp
    &pmi8950_charger {
	    qcom,battery-data = <&qrd_batterydata>;
	    qcom,float-voltage-mv = <4350>;
	    qcom,chg-led-sw-controls; 
        qcom,chg-led-support;       
	    status = "okay";
    };
```
(3)解析dtsi文件的相关代码
配置三色灯属性，
相关代码见kernel/drivers/leds/leds-aw2013.c中的aw2013_led_probe
```cpp
static int aw2013_led_probe(struct i2c_client *client,
			                const struct i2c_device_id *id)
{
	struct aw2013_led *led_array;
	struct device_node *node;
	
	ret = aw2013_led_parse_child_node(led_array, node);
	if (ret) {
		dev_err(&client->dev, "parsed node error\n");
		goto free_led_arry;
	}
}

static int aw2013_led_parse_child_node(struct aw2013_led *led_array,
				                       struct device_node *node)
{	
	for_each_child_of_node(node, temp) {
		rc = of_property_read_string(temp, "aw2013,name",
			&led->cdev.name);
		if (rc < 0) {
			dev_err(&led->client->dev,
				"Failure reading led name, rc = %d\n", rc);
			goto free_pdata;
		}

		rc = of_property_read_u32(temp, "aw2013,id",
			&led->id);
		if (rc < 0) {
			dev_err(&led->client->dev,
				"Failure reading id, rc = %d\n", rc);
			goto free_pdata;
		}

		rc = of_property_read_u32(temp, "aw2013,max-brightness",
			&led->cdev.max_brightness);
		if (rc < 0) {
			dev_err(&led->client->dev,
				"Failure reading max-brightness, rc = %d\n",
				rc);
			goto free_pdata;
		}

		rc = of_property_read_u32(temp, "aw2013,max-current",
			&led->pdata->max_current);
		if (rc < 0) {
			dev_err(&led->client->dev,
				"Failure reading max-current, rc = %d\n", rc);
			goto free_pdata;
		}

		rc = of_property_read_u32(temp, "aw2013,rise-time-ms",
			&led->pdata->rise_time_ms);
		if (rc < 0) {
			dev_err(&led->client->dev,
				"Failure reading rise-time-ms, rc = %d\n", rc);
			goto free_pdata;
		}

		rc = of_property_read_u32(temp, "aw2013,hold-time-ms",
			&led->pdata->hold_time_ms);
		if (rc < 0) {
			dev_err(&led->client->dev,
				"Failure reading hold-time-ms, rc = %d\n", rc);
			goto free_pdata;
		}

		rc = of_property_read_u32(temp, "aw2013,fall-time-ms",
			&led->pdata->fall_time_ms);
		if (rc < 0) {
			dev_err(&led->client->dev,
				"Failure reading fall-time-ms, rc = %d\n", rc);
			goto free_pdata;
		}

		rc = of_property_read_u32(temp, "aw2013,off-time-ms",
			&led->pdata->off_time_ms);
		if (rc < 0) {
			dev_err(&led->client->dev,
				"Failure reading off-time-ms, rc = %d\n", rc);
			goto free_pdata;
		}

		INIT_WORK(&led->brightness_work, aw2013_brightness_work);

		led->cdev.brightness_set = aw2013_set_brightness;	
	}
}
```
charger led support property是个什么属性？不支持反向充电功能，QPNP是个什么能力。
相关代码在kernel/drivers/power/qpnp-smbcharger.c中。
```cpp
static int smbchg_probe(struct spmi_device *spmi)
{
    struct smbchg_chip *chip;
    
    rc = smb_parse_dt(chip);
	if (rc < 0) {
		dev_err(&spmi->dev, "Unable to parse DT nodes: %d\n", rc);
		return rc;
	}
	
    if (chip->cfg_chg_led_support &&
			chip->schg_version == QPNP_SCHG_LITE) {
		rc = smbchg_register_chg_led(chip);
		if (rc) {
			dev_err(chip->dev,
					"Unable to register charger led: %d\n",
					rc);
			goto unregister_dc_psy;
		}

		rc = smbchg_chg_led_controls(chip);
		if (rc) {
			dev_err(chip->dev,
					"Failed to set charger led controld bit: %d\n",
					rc);
			goto unregister_led_class;
		}
	}
}

static int smb_parse_dt(struct smbchg_chip *chip)
{
    chip->cfg_chg_led_support = 
		of_property_read_bool(node, "qcom,chg-led-support");
}
```
不支持mpp
相关代码在kernel/drivers/power/leds-qpnp.c中。
```cpp
static int qpnp_leds_probe(struct spmi_device *spmi)
{
	struct qpnp_led_data *led, *led_array;	
	struct device_node *node, *temp;
	
	for_each_child_of_node(node, temp) {
		rc = of_property_read_string(temp, "label", &led_label);
		if (rc < 0) {
			dev_err(&led->spmi_dev->dev,
				"Failure reading label, rc = %d\n", rc);
			goto fail_id_check;
		}

		rc = of_property_read_string(temp, "linux,name",
			&led->cdev.name);
		if (rc < 0) {
			dev_err(&led->spmi_dev->dev,
				"Failure reading led name, rc = %d\n", rc);
			goto fail_id_check;
		}

		rc = of_property_read_u32(temp, "qcom,max-current",
			&led->max_current);
		if (rc < 0) {
			dev_err(&led->spmi_dev->dev,
				"Failure reading max_current, rc =  %d\n", rc);
			goto fail_id_check;
		}

		rc = of_property_read_u32(temp, "qcom,id", &led->id);
		if (rc < 0) {
			dev_err(&led->spmi_dev->dev,
				"Failure reading led id, rc =  %d\n", rc);
			goto fail_id_check;
		}

		rc = qpnp_get_common_configs(led, temp);
		if (rc) {
			dev_err(&led->spmi_dev->dev,
				"Failure reading common led configuration," \
				" rc = %d\n", rc);
			goto fail_id_check;
		}		

		if (strncmp(led_label, "mpp", sizeof("mpp")) == 0) {
			rc = qpnp_get_config_mpp(led, temp);
			if (rc < 0) {
				dev_err(&led->spmi_dev->dev,
						"Unable to read mpp config data\n");
				goto fail_id_check;
			}
		} 		

		INIT_WORK(&led->work, qpnp_led_work);		

		if (led->id == QPNP_ID_LED_MPP) {
			if (!led->mpp_cfg->pwm_cfg)
				break;
			if (led->mpp_cfg->pwm_cfg->mode == PWM_MODE) {
				rc = sysfs_create_group(&led->cdev.dev->kobj,
					&pwm_attr_group);
				if (rc)
					goto fail_id_check;
			}
			if (led->mpp_cfg->pwm_cfg->use_blink) {
				rc = sysfs_create_group(&led->cdev.dev->kobj,
					&blink_attr_group);
				if (rc)
					goto fail_id_check;

				rc = sysfs_create_group(&led->cdev.dev->kobj,
					&lpg_attr_group);
				if (rc)
					goto fail_id_check;
			} else if (led->mpp_cfg->pwm_cfg->mode == LPG_MODE) {
				rc = sysfs_create_group(&led->cdev.dev->kobj,
					&lpg_attr_group);
				if (rc)
					goto fail_id_check;
			}
		} 
	}		
}

static int qpnp_get_config_mpp(struct qpnp_led_data *led,
		                       struct device_node *node)
{
	led->mpp_cfg->current_setting = LED_MPP_CURRENT_MIN;
	rc = of_property_read_u32(node, "qcom,current-setting", &val);
	if (!rc) {
		if (led->mpp_cfg->current_setting < LED_MPP_CURRENT_MIN)
			led->mpp_cfg->current_setting = LED_MPP_CURRENT_MIN;
		else if (led->mpp_cfg->current_setting > LED_MPP_CURRENT_MAX)
			led->mpp_cfg->current_setting = LED_MPP_CURRENT_MAX;
		else
			led->mpp_cfg->current_setting = (u8) val;
	} else if (rc != -EINVAL)
		goto err_config_mpp;

	led->mpp_cfg->source_sel = LED_MPP_SOURCE_SEL_DEFAULT;
	rc = of_property_read_u32(node, "qcom,source-sel", &val);
	if (!rc)
		led->mpp_cfg->source_sel = (u8) val;
	else if (rc != -EINVAL)
		goto err_config_mpp;

	led->mpp_cfg->mode_ctrl = LED_MPP_MODE_SINK;
	rc = of_property_read_u32(node, "qcom,mode-ctrl", &val);
	if (!rc)
		led->mpp_cfg->mode_ctrl = (u8) val;
	else if (rc != -EINVAL)
		goto err_config_mpp;	
	
	rc = of_property_read_string(node, "qcom,mode", &mode);
	if (!rc) {
		led_mode = qpnp_led_get_mode(mode);
		led->mpp_cfg->pwm_mode = led_mode;
		if (led_mode == MANUAL_MODE)
			return MANUAL_MODE;
		else if (led_mode == -EINVAL) {
			dev_err(&led->spmi_dev->dev, "Selected mode not " \
				"supported for mpp.\n");
			rc = -EINVAL;
			goto err_config_mpp;
		}
		led->mpp_cfg->pwm_cfg = devm_kzalloc(&led->spmi_dev->dev,
					sizeof(struct pwm_config_data),
					GFP_KERNEL);
		if (!led->mpp_cfg->pwm_cfg) {
			dev_err(&led->spmi_dev->dev,
				"Unable to allocate memory\n");
			rc = -ENOMEM;
			goto err_config_mpp;
		}
		led->mpp_cfg->pwm_cfg->mode = led_mode;
		led->mpp_cfg->pwm_cfg->default_mode = led_mode;
	} else
		return rc;	
}
```
也不支持wled
相关代码见/kernel/drivers/leds/leds-qpnp-wled.c
```cpp
static int qpnp_wled_probe(struct spmi_device *spmi)
{
	struct qpnp_wled *wled;

	rc = qpnp_wled_parse_dt(wled);
	if (rc) {
		dev_err(&spmi->dev, "DT parsing failed\n");
		return rc;
	}	
}

static int qpnp_wled_parse_dt(struct qpnp_wled *wled)
{
	struct spmi_device *spmi = wled->spmi;
	

	wled->fs_curr_ua = QPNP_WLED_FS_CURR_MAX_UA;
	rc = of_property_read_u32(spmi->dev.of_node,
			"qcom,fs-curr-ua", &temp_val);
	if (!rc) {
		wled->fs_curr_ua = temp_val;
	} else if (rc != -EINVAL) {
		dev_err(&spmi->dev, "Unable to read full scale current\n");
		return rc;
	}

        return 0;	
}
```



