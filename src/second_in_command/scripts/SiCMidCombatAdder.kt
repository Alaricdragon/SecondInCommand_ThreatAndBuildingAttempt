package second_in_command.scripts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.mission.FleetSide
import org.apache.log4j.Logger
import second_in_command.SCData
import second_in_command.hullmods.SCControllerHullmod
import second_in_command.hullmods.SCControllerHullmod.Companion.noSkillTagHullmodID

class SiCMidCombatAdder : BaseEveryFrameCombatPlugin(){
    init {
        map = HashMap<Int?, SCData?>();
        shipsToAdd = HashMap<ShipAPI?, SCData?>()
    }
    companion object {
        var map: HashMap<Int?, SCData?> = HashMap<Int?, SCData?>()
        var shipsToAdd: HashMap<ShipAPI?, SCData?> = HashMap<ShipAPI?, SCData?>()
    }
    var cooldown: Float = 1f
    var time: Float = cooldown
    override fun advance(amount: Float, events: MutableList<InputEventAPI?>?) {
        time -= amount
        if (time > 0) return
        time = cooldown
        addShipsSCDATA()
        //for ()
        //events.get(0).getEventType().equals(CombatEvent);
        val engine = Global.getCombatEngine()
        for (a in engine.ships) {
            //log?.info("isValidShipToConvert: name: "+a.name+" id: "+a.id + "hullID: "+a.hullSpec.hullId);
            if (!isValidShipToConvert(a)) continue;
            //log?.info(" isValidShipToConvert: is valid ship");
            //attempt to acquire data from any possible 'warrant', or other source such as fleet member that might be set, before resorting to looking for
            var data: SCData? = SCControllerHullmod.getFleetData(a);
            //log?.info(" isValidShipToConvert: got data from other source?"+(data != null));
            val force = a.originalOwner
            if (data == null && map.containsKey(force)) data = map.get(force);
            Global.getCombatEngine().getFleetManager(FleetSide.ENEMY)
            if (data == null){
                continue
            }
            //log?.info(" isValidShipToConvert: has item in map. is data null: "+(map.get(force)==null));
            refitShip(a,map.get(force));
            //addAllIncludingKids(a, map.get(force))
        }
        //events.get(0).getEventClass().equals()
    }
    private fun addShipsSCDATA(){
        var toRemove = ArrayList<ShipAPI?>()
        for (a in shipsToAdd.keys){
            if (a?.originalOwner != -1){
                toRemove.add(a);
                map.put(a?.originalOwner,shipsToAdd.get(a));
            };
        }
        for (a in toRemove) shipsToAdd.remove(a);
    }

    private fun addAllIncludingKids(shipAPI: ShipAPI, data: SCData?) {
        //note on kids: this is disabled because the search plugin will find them anyways (SiCMidCombatAdder). Please find a way to add without freezes (I messed up a while loop somewere)
        //refitShip(shipAPI, data)
        //val log: Logger? = Global.getLogger(SCControllerHullmod::class.java)
        val childs = ArrayList<ShipAPI?>()
        childs.addAll(shipAPI.childModulesCopy)
        //log?.info("attempting to add kids. Kids size is: "+childs.size);
        while (!childs.isEmpty()) {
            var looking: ShipAPI? = childs[0];
            //log?.info("size is: "+childs.size);
            childs.removeAt(0)
            if (looking == null) continue;
            if (looking.customData?.containsKey(SCControllerHullmod.secOverrideKey) == true) continue
            //log?.info("size after 1 removed is: "+childs.size);
            //child ships can have child ships. destroy them
            if (looking.childModulesCopy != null && !looking.childModulesCopy.isEmpty()) childs.addAll(looking.childModulesCopy)
            //log?.info("size after all possable added is: "+childs.size);
            //NanoThief_BattleListener.reclaimOverride.put(childs.get(0), (int) (0));
            refitShip(looking, data)
        }
    }
    private fun isValidShipToConvert(a: ShipAPI) : Boolean{
        if (a.isHulk) return false;
        if (!a.isAlive) return false;
        if (a.hullSize == ShipAPI.HullSize.FIGHTER) return false;
        if (a.variant.hasHullMod("sc_skill_controller") && a.fleetMember.fleetData != null) return false;
        if (a.customData.contains(SCControllerHullmod.secOverrideKey) && a.variant.hasHullMod("sc_skill_controller")) return false;
        if (a.variant.hasHullMod(noSkillTagHullmodID)) return false;
        if (a.variant.hasTag(noSkillTagHullmodID)) return false;
        return true;
    }
    private fun refitShip(shipAPI: ShipAPI, data: SCData?) {
        if (data == null) return
        SCControllerHullmod.addHullmodAfterShipCreation(shipAPI, data);
    }
}