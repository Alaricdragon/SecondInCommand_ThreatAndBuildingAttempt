package second_in_command.scripts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import org.apache.log4j.Logger
import second_in_command.SCData
import second_in_command.hullmods.SCControllerHullmod
import second_in_command.hullmods.SCControllerHullmod.Companion.noSkillTagHullmodID

class SiCMidCombatAdder : BaseEveryFrameCombatPlugin(){
    init {
        map = HashMap<Int?, SCData?>();
    }
    companion object {
        var map: HashMap<Int?, SCData?> = HashMap<Int?, SCData?>()
    }
    var cooldown: Float = 0.5f
    var time: Float = cooldown
    override fun advance(amount: Float, events: MutableList<InputEventAPI?>?) {
        time -= amount
        if (time > 0) return
        time = cooldown
        //for ()
        //events.get(0).getEventType().equals(CombatEvent);
        val engine = Global.getCombatEngine()
        for (a in engine.ships) {
            if (!isValidShipToConvert(a)) continue;
            val force = a.originalOwner
            if (map.containsKey(force)) refitShip(a,map.get(force));
            /*for (b in engine.ships) {
                if (b.originalOwner != force) continue
                if (b.fleetMember != null && b.fleetMember.fleetData != null && b.fleetMember.fleetData.fleet != null && SCUtils.getFleetData(b.fleetMember.fleetData.fleet) != null) {
                    refitShip(a, b.fleetMember.fleetData.fleet)
                    //time = -1;//break this, then run it again immanently.
                    //return;
                    break
                }
            }*/
        }
        //events.get(0).getEventClass().equals()
    }

    private fun addAllIncludingKids(shipAPI: ShipAPI, data: SCData) {
        //refitShip(shipAPI, data)
        val log: Logger? = Global.getLogger(SCControllerHullmod::class.java)
        val childs = ArrayList<ShipAPI?>()
        childs.addAll(shipAPI.childModulesCopy)
        log?.info("attempting to add kids. Kids size is: "+childs.size);
        while (!childs.isEmpty()) {
            var looking: ShipAPI? = childs[0];
            log?.info("size is: "+childs.size);
            childs.removeAt(0)
            log?.info("size after 1 removed is: "+childs.size);
            if (looking == null) continue;
            //child ships can have child ships. destroy them
            if (looking.childModulesCopy != null && looking.childModulesCopy.isEmpty()) childs.addAll(looking.childModulesCopy)
            log?.info("size after all possable added is: "+childs.size);
            //NanoThief_BattleListener.reclaimOverride.put(childs.get(0), (int) (0));
            refitShip(looking, data)
        }
    }
    private fun isValidShipToConvert(a: ShipAPI) : Boolean{
        if (a.isHulk) return false;
        if (a.hullSize == ShipAPI.HullSize.FIGHTER) return false;
        if (a.variant.hasHullMod("sc_skill_controller") && (a.fleetMember.fleetData != null || a.customData.contains(SCControllerHullmod.secOverrideKey))) return false;
        if (a.variant.hasHullMod(noSkillTagHullmodID)) return false;
        if (a.variant.hasTag(noSkillTagHullmodID)) return false;
        return true;
    }
    private fun refitShip(shipAPI: ShipAPI, data: SCData?) {
        if (data == null) return
        SCControllerHullmod.addHullmodAfterShipCreation(shipAPI, data);
        addAllIncludingKids(shipAPI, data)
    }
}