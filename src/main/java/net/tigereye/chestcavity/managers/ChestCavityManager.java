package net.tigereye.chestcavity.managers;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.tigereye.chestcavity.ChestCavity;
import net.tigereye.chestcavity.items.*;
import net.tigereye.chestcavity.listeners.OrganTickCallback;
import net.tigereye.chestcavity.listeners.OrganUpdateCallback;
import net.tigereye.chestcavity.registration.CCOrganScores;
import net.tigereye.chestcavity.registration.CCOtherOrgans;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ChestCavityManager implements InventoryChangedListener {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final Identifier COMPATIBILITY_TAG = new Identifier(ChestCavity.MODID,"organ_compatibility");
    public static final int COMPATIBILITY_TYPE_PERSONAL = 1;
    public static final int COMPATIBILITY_TYPE_SPECIES = 2;
    protected static final Map<Identifier,Float> defaultOrganScores = new HashMap<>();
    protected LivingEntity owner;
    protected ChestCavityInventory chestCavity;
    protected Map<Identifier,Float> oldOrganScores = new HashMap<>();
    protected Map<Identifier,Float> organScores = new HashMap<>();

    protected boolean opened = false;
    protected int heartTimer = 0;
    protected int kidneyTimer = 0;
    protected int liverTimer = 0;
    protected int spleenTimer = 0;
    protected float lungRemainder = 0;

    static{
        initializeDefaultOrgans();
    }

    public ChestCavityManager(LivingEntity owner){
        this.chestCavity = new ChestCavityInventory(27,this);
        this.owner = owner;
        LOGGER.debug("[Chest Cavity] Initializing ChestCavityManager");

    }
    public ChestCavityManager(LivingEntity owner, int size){
        this.chestCavity = new ChestCavityInventory(size,this);
        this.owner = owner;
        LOGGER.debug("[Chest Cavity] Initializing ChestCavityManager");
    }

    public void init(){
        if(!owner.getEntityWorld().isClient()) {
            //generateChestCavity();
            evaluateChestCavity();
        }
        getChestCavity().addListener(this);
    }

    protected static void initializeDefaultOrgans(){
        defaultOrganScores.put(CCOrganScores.APPENDIX,1f);
        defaultOrganScores.put(CCOrganScores.BONE,4.75f);
        defaultOrganScores.put(CCOrganScores.HEART,1f);
        defaultOrganScores.put(CCOrganScores.INTESTINE,4f);
        defaultOrganScores.put(CCOrganScores.KIDNEY,2f);
        defaultOrganScores.put(CCOrganScores.LIVER,1f);
        defaultOrganScores.put(CCOrganScores.LUNG,2f);
        defaultOrganScores.put(CCOrganScores.STRENGTH,8f);
        defaultOrganScores.put(CCOrganScores.SPEED,8f);
        defaultOrganScores.put(CCOrganScores.NERVOUS_SYSTEM,1f);
        defaultOrganScores.put(CCOrganScores.SPLEEN,1f);
        defaultOrganScores.put(CCOrganScores.STOMACH,1f);
    }

    public ChestCavityInventory getChestCavity() {
        return chestCavity;
    }

    public void setOwner(LivingEntity owner){
        this.owner = owner;
    }

    public boolean getOpened(){
        return this.opened;
    }

    public void setOpened(boolean b) {
        this.opened = b;
    }

    public int getHeartTimer() {
        return heartTimer;
    }

    public void setHeartTimer(int heartTimer) {
        this.heartTimer = heartTimer;
    }

    public int getKidneyTimer() {
        return kidneyTimer;
    }

    public void setKidneyTimer(int kidneyTimer) {
        this.kidneyTimer = kidneyTimer;
    }

    public int getLiverTimer() {
        return liverTimer;
    }

    public void setLiverTimer(int liverTimer) {
        this.liverTimer = liverTimer;
    }

    public int getSpleenTimer() {
        return spleenTimer;
    }

    public void setSpleenTimer(int spleenTimer) {
        this.spleenTimer = spleenTimer;
    }

    public float getLungRemainder() {
        return lungRemainder;
    }

    public void setLungRemainder(int lungRemainder) {
        this.lungRemainder = lungRemainder;
    }

    public float getOrganScore(Identifier id) {
        return organScores.getOrDefault(id, 0f);
    }

    public void setOrganScore(Identifier id, float value){
        organScores.put(id,value);
    }

    public void addOrganScore(Identifier id, float value){
        organScores.put(id,organScores.getOrDefault(id,0f)+value);
    }
    @Override
    public void onInventoryChanged(Inventory sender) {
        evaluateChestCavity();
    }
    public void evaluateChestCavity() {
        if(!opened){
            organScores.clear();
            organScores.putAll(defaultOrganScores);
        }
        else {
            resetOrganScores();

            for (int i = 0; i < chestCavity.size(); i++) {
                ItemStack itemStack = chestCavity.getStack(i);
                if (itemStack != null && itemStack != ItemStack.EMPTY) {
                    Item slotitem = itemStack.getItem();
                    if (!catchExceptionalOrgan(itemStack)) {//if a manager chooses to handle some organ in a special way, this lets it skip the normal evaluation.
                        Map<Identifier, Float> organMap = lookupOrganScore(itemStack);
                        if (lookupOrganScore(itemStack) != null) {
                            organMap.forEach((key, value) ->
                                    addOrganScore(key, value * itemStack.getCount() / itemStack.getMaxCount()));
                        }
                    }

                    CompoundTag tag = itemStack.getTag();
                    if (tag != null && tag.contains(COMPATIBILITY_TAG.toString())) {
                        tag = tag.getCompound(COMPATIBILITY_TAG.toString());
                        if (tag.getInt("type") == COMPATIBILITY_TYPE_PERSONAL) {
                            if (!tag.getUuid("owner").equals(owner.getUuid())) {
                                if (ChestCavity.DEBUG_MODE && owner instanceof PlayerEntity) {
                                    System.out.println("incompatability found! item bound to UUID " + tag.getUuid("owner").toString() + " but player is UUID " + owner.getUuid());
                                }
                                addOrganScore(CCOrganScores.INCOMPATIBILITY, 1);
                            }
                        } else if (tag.getInt("type") == COMPATIBILITY_TYPE_SPECIES) {
                            //TODO: implement species compatibility
                            //if(tag.getUuid("owner") != owner.getUuid()){
                            //    addOrganScore(CCOrganScores.INCOMPATIBILITY,.5f);
                            //}
                        }
                    }
                }
            }
        }
        organUpdate();
    }

    protected Map<Identifier,Float> lookupOrganScore(ItemStack itemStack){
        Item item = itemStack.getItem();
        if(item instanceof ChestCavityOrgan){
            return ((ChestCavityOrgan) item).getOrganQualityMap(itemStack, owner);
        }
        else if(CCOtherOrgans.map.containsKey(item)){
                return CCOtherOrgans.map.get(item);
        }
        return null;
    }

    protected void organUpdate(){
        if(!oldOrganScores.equals(organScores))
        {
            if(ChestCavity.DEBUG_MODE && owner instanceof PlayerEntity) {
                try {
                    Text name = owner.getName();
                    System.out.println("[Chest Cavity] Displaying " + name.getString() +"'s organ scores:");
                }
                catch(Exception e){
                    System.out.println("[Chest Cavity] Displaying organ scores:");
                }
                organScores.forEach((key, value) ->
                        System.out.print(key.toString() + ": " + value + " "));
                System.out.print("\n");
            }

            OrganUpdateCallback.EVENT.invoker().onOrganUpdate(owner, oldOrganScores, organScores);
            oldOrganScores.clear();
            oldOrganScores.putAll(organScores);
        }
    }

    protected void resetOrganScores(){
        organScores.clear();
    }

    protected boolean catchExceptionalOrgan(ItemStack slot){
        return false;
    }

    public void onTick(){
        OrganTickCallback.EVENT.invoker().onOrganTick(owner, this);
        organUpdate();
    }

    public ChestCavityInventory openChestCavity(){
        if(!opened) {
            getChestCavity().removeListener(this); //just in case really
            opened = true;
            generateChestCavity();
            getChestCavity().addListener(this);
        }
        return chestCavity;
    }

    protected void generateChestCavity(){
        if(opened) {
            fillChestCavityInventory();
            //TODO: add event where listeners can overwrite specific organs before compatibility is set
            setOrganCompatibility();
        }
    }

    protected void fillChestCavityInventory() {
        chestCavity.clear();
        for(int i = 0; i < chestCavity.size(); i++){
            chestCavity.setStack(i,new ItemStack(Items.DIRT,64));
        }
    }

    protected void setOrganCompatibility(){
        //first, make all organs personal
        for(int i = 0; i < chestCavity.size();i++){
            ItemStack itemStack = chestCavity.getStack(i);
            if(itemStack != null && itemStack != itemStack.EMPTY){
                CompoundTag tag = new CompoundTag();
                tag.putInt("type", COMPATIBILITY_TYPE_PERSONAL);
                tag.putUuid("owner",owner.getUuid());
                itemStack.putSubTag(COMPATIBILITY_TAG.toString(),tag);
            }
        }
        //then check if any may become more compatible, and if so how many attempts will be made
        int universalOrgans = 0;
        //int communalOrgans = 0;
        Random random = owner.getRandom();
        if(random.nextFloat() < ChestCavity.config.UNIVERSAL_DONOR_RATE){
            universalOrgans = 1+random.nextInt(3)+random.nextInt(3);
        }
        /*communalOrgans = 1+random.nextInt(4)+random.nextInt(4);
        while(communalOrgans > 0){
            int i = random.nextInt(chestCavity.size());
            ItemStack itemStack = chestCavity.getStack(i);
            if(itemStack != null){
                CompoundTag tag = new CompoundTag();
                tag.putInt("compatibility_type", COMPATIBILITY_TYPE_SPECIES);
                tag.putString("species",owner.getType().tag);
                itemStack.putSubTag(COMPATIBILITY_TAG.toString(),tag);
            }
            communalOrgans--;
        }*/
        //each attempt, roll a random slot in the chestcavity and turn that organ, if any, compatible
        while(universalOrgans > 0){
            int i = random.nextInt(chestCavity.size());
            ItemStack itemStack = chestCavity.getStack(i);
            if(itemStack != null && itemStack != ItemStack.EMPTY){
                itemStack.removeSubTag(COMPATIBILITY_TAG.toString());
            }
            universalOrgans--;
        }
    }

    public void chestCavityPostMortem(){
        //TODO: check if target is unopened before this step
        dropUnboundOrgans();
    }

    public List<ItemStack> generateLootDrops(Random random, int looting){
        return new ArrayList<>();
    }

    protected void dropUnboundOrgans(){
        chestCavity.removeListener(this);
        for(int i = 0; i < chestCavity.size(); i++){
            ItemStack itemStack = chestCavity.getStack(i);
            if(itemStack != null && itemStack != itemStack.EMPTY) {
                CompoundTag tag = itemStack.getTag();
                if (tag != null && tag.contains(COMPATIBILITY_TAG.toString())) {
                    tag = tag.getCompound(COMPATIBILITY_TAG.toString());
                    if (tag.getInt("type") == COMPATIBILITY_TYPE_PERSONAL) {
                        if (!tag.getUuid("owner").equals(owner.getUuid())) {
                            //drop item
                            owner.dropStack(chestCavity.removeStack(i));
                        }
                    } else {
                        owner.dropStack(chestCavity.removeStack(i));
                    }
                } else {
                    owner.dropStack(chestCavity.removeStack(i));
                }
            }
        }
        chestCavity.addListener(this);
        evaluateChestCavity();
    }

    public void fromTag(CompoundTag tag, LivingEntity owner) {
        LOGGER.debug("[Chest Cavity] Reading ChestCavityManager fromTag");
        this.owner = owner;
        if(tag.contains("ChestCavity")){
            if(ChestCavity.DEBUG_MODE) {
                System.out.println("Found Save Data");
            }
            CompoundTag ccTag = tag.getCompound("ChestCavity");
            this.opened = ccTag.getBoolean("opened");
            this.heartTimer = ccTag.getInt("HeartTimer");
            this.kidneyTimer = ccTag.getInt("KidneyTimer");
            this.liverTimer = ccTag.getInt("LiverTimer");
            this.spleenTimer = ccTag.getInt("SpleenTimer");
            this.lungRemainder = ccTag.getFloat("LungRemainder");
            chestCavity.removeListener(this);
            if (ccTag.contains("Inventory")) {
                ListTag listTag = ccTag.getList("Inventory", 10);
                this.chestCavity.readTags(listTag);
            }
            else if(opened){
                LOGGER.warn("[Chest Cavity] "+owner.getName().asString()+"'s Chest Cavity is mangled. It will be replaced");
                generateChestCavity();
            }
            chestCavity.addListener(this);
        }
        else if(tag.contains("cardinal_components")){
                CompoundTag temp = tag.getCompound("cardinal_components");
                if(temp.contains("chestcavity:inventorycomponent")){
                    temp = tag.getCompound("chestcavity:inventorycomponent");
                    if(temp.contains("chestcavity")){
                        LOGGER.info("[Chest Cavity] Found "+owner.getName().asString()+"'s old Chest Cavity (v1).");
                        opened = true;
                        ListTag listTag = temp.getList("Inventory", 10);
                        chestCavity.removeListener(this);
                        this.chestCavity.readTags(listTag);
                        chestCavity.addListener(this);
                    }
                }
        }
        evaluateChestCavity();
    }

    public void toTag(CompoundTag tag) {
        if(ChestCavity.DEBUG_MODE) {
            System.out.println("Writing ChestCavityManager toTag");
        }
        CompoundTag ccTag = new CompoundTag();
        ccTag.putBoolean("opened", this.opened);
        ccTag.putInt("HeartTimer", this.heartTimer);
        ccTag.putInt("KidneyTimer", this.kidneyTimer);
        ccTag.putInt("LiverTimer", this.liverTimer);
        ccTag.putInt("SpleenTimer", this.spleenTimer);
        ccTag.putFloat("LungRemainder", this.lungRemainder);
        ccTag.put("Inventory", this.chestCavity.getTags());
        tag.put("ChestCavity",ccTag);
    }

    public void clone(ChestCavityManager other) {
        opened = other.getOpened();
        chestCavity.removeListener(this);
        for(int i = 0; i < this.chestCavity.size(); ++i) {
            this.chestCavity.setStack(i, other.getChestCavity().getStack(i));
        }
        chestCavity.addListener(this);

        heartTimer = other.getHeartTimer();
        liverTimer = other.getLiverTimer();
        kidneyTimer = other.getKidneyTimer();
        spleenTimer = other.getSpleenTimer();
        lungRemainder = other.getLungRemainder();
        evaluateChestCavity();
    }

    public float applyBoneDefense(float damage){
        float boneScore = organScores.getOrDefault(CCOrganScores.BONE,0f);
        return damage*(5/(.25f+boneScore));
        //normal bone score of 4.75 means no change
    }

    public float applyIntestinesSaturation(float sat){
        return sat*organScores.getOrDefault(CCOrganScores.INTESTINE,0f)/4;
    }

    public int applyStomachHunger(int hunger){
        //sadly, in order to get saturation at all we must grant at least half a haunch of food, unless we embrace incompatability
        return Math.max((int)(hunger*organScores.getOrDefault(CCOrganScores.STOMACH,0f)),1);
    }

    public int applyLungCapacityInWater(){
        float airloss = 2f/Math.max(organScores.getOrDefault(CCOrganScores.LUNG,0f),.1f) + lungRemainder;
        lungRemainder = airloss % 1;
        return (int) airloss;
    }

    public int applySpleenMetabolism(int foodStarvationTimer){
        spleenTimer++;
        if(spleenTimer >=2){
            foodStarvationTimer += organScores.getOrDefault(CCOrganScores.SPLEEN,0f) - 1;
        }
        spleenTimer = 0;
        return foodStarvationTimer;
    }

}
