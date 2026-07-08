package second_in_command.scripts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.threat.ThreatShipConstructionScript
import com.fs.starfarer.api.input.InputEventAPI
import second_in_command.SCUtils
import second_in_command.hullmods.SCControllerHullmod
import second_in_command.hullmods.SCControllerHullmod.Companion.noSkillTagHullmodID

class SiCMidCombatAdder : BaseEveryFrameCombatPlugin(){
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
            for (b in engine.ships) {
                if (b.originalOwner != force) continue
                if (b.fleetMember != null && b.fleetMember.fleetData != null && b.fleetMember.fleetData.fleet != null && SCUtils.getFleetData(b.fleetMember.fleetData.fleet) != null) {
                    refitShip(a, b.fleetMember.fleetData.fleet)
                    //time = -1;//break this, then run it again immanently.
                    //return;
                    break
                }
            }
        }
        //events.get(0).getEventClass().equals()
    }

    private fun addAllIncludingKids(shipAPI: ShipAPI, fleet: CampaignFleetAPI?) {
        refitShip(shipAPI, fleet)
        val childs = ArrayList<ShipAPI?>()
        childs.addAll(shipAPI.childModulesCopy)
        while (!childs.isEmpty()) {
            //child ships can have child ships. destroy them
            if (childs[0]!!.childModulesCopy != null && !childs.get(0)!!.childModulesCopy
                    .isEmpty()
            ) childs.addAll(childs[0]!!.childModulesCopy)
            //NanoThief_BattleListener.reclaimOverride.put(childs.get(0), (int) (0));
            refitShip(childs[0]!!, fleet)
            childs.removeAt(0)
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
    private fun refitShip(shipAPI: ShipAPI, fleet: CampaignFleetAPI?) {
        SCControllerHullmod.addHullmodAfterShipCreation(shipAPI, fleet);
        addAllIncludingKids(shipAPI, fleet)
    }
}