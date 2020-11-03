package net.tigereye.chestcavity.registration;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.tigereye.chestcavity.registration.CCOrganScores;

import java.util.HashMap;
import java.util.Map;

public class CCOtherOrgans {

    public static Map<Item,Map<Identifier,Float>> map = new HashMap<>();

    public static void init(){
        Map<Identifier,Float> dirt = new HashMap<>();
        dirt.put(CCOrganScores.APPENDIX,1f/27);
        dirt.put(CCOrganScores.HEART,1f/27);
        dirt.put(CCOrganScores.STRENGTH,8f/27);
        dirt.put(CCOrganScores.SPEED,8f/27);
        dirt.put(CCOrganScores.NERVOUS_SYSTEM,1f/27);
        dirt.put(CCOrganScores.LIVER,1f/27);
        dirt.put(CCOrganScores.KIDNEY,2f/27);
        dirt.put(CCOrganScores.SPLEEN,1f/27);
        dirt.put(CCOrganScores.LUNG,2f/27);
        dirt.put(CCOrganScores.INTESTINE,4f/27);
        dirt.put(CCOrganScores.BONE,4f/27);
        dirt.put(CCOrganScores.STOMACH,1f/27);
        Map<Identifier,Float> rottenFlesh = new HashMap<>();
        rottenFlesh.put(CCOrganScores.STRENGTH,.5f);
        rottenFlesh.put(CCOrganScores.SPEED,.5f);
        Map<Identifier,Float> animalFlesh = new HashMap<>();
        animalFlesh.put(CCOrganScores.STRENGTH,.75f);
        animalFlesh.put(CCOrganScores.SPEED,.75f);
        Map<Identifier,Float> bone = new HashMap<>();
        rottenFlesh.put(CCOrganScores.BONE,.5f);
        map.put(Items.DIRT,dirt);
        map.put(Items.ROTTEN_FLESH,rottenFlesh);
        map.put(Items.BEEF,animalFlesh);
        map.put(Items.PORKCHOP,animalFlesh);
        map.put(Items.MUTTON,animalFlesh);
        map.put(Items.BONE,bone);
    }
}