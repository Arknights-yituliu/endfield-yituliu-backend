package org.yituliu.entity.vo;


import org.yituliu.entity.po.CharacterPoolRecord;
import org.yituliu.entity.po.WeaponPoolRecord;

import java.util.List;


public class PoolRecordVO {
    private List<CharacterPoolRecord> characterPoolRecord;
    private List<WeaponPoolRecord> weaponPoolRecord;

    public PoolRecordVO() {
    }

    public PoolRecordVO(List<CharacterPoolRecord> characterPoolRecord, List<WeaponPoolRecord> weaponPoolRecord) {
        this.characterPoolRecord = characterPoolRecord;
        this.weaponPoolRecord = weaponPoolRecord;
    }

    public List<CharacterPoolRecord> getCharacterPoolRecord() {
        return characterPoolRecord;
    }

    public void setCharacterPoolRecord(List<CharacterPoolRecord> characterPoolRecord) {
        this.characterPoolRecord = characterPoolRecord;
    }

    public List<WeaponPoolRecord> getWeaponPoolRecord() {
        return weaponPoolRecord;
    }

    public void setWeaponPoolRecord(List<WeaponPoolRecord> weaponPoolRecord) {
        this.weaponPoolRecord = weaponPoolRecord;
    }
}


