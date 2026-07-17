package second_in_command.scripts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.listeners.FleetMemberDeploymentListener
import second_in_command.SCData
import second_in_command.hullmods.SCControllerHullmod
import second_in_command.hullmods.SCControllerHullmod.Companion.noSkillTagHullmodID
import kotlin.collections.contains
import org.apache.log4j.Logger

class SicMidCombatAdder2 : FleetMemberDeploymentListener{
    var addModules = Global.getSettings().getBoolean("sc_applyToModules");
    init{
        //val log: Logger? = Global.getLogger(SCControllerHullmod::class.java)
        var map: HashMap<Int?, SCData?> = HashMap<Int?, SCData?>()
        val engine = Global.getCombatEngine()
        //this loop is here because this plugin is added after the first ship is created. basicly gets all 'on spawned' ships and adds them to relevant listeners.
        for (a in engine.ships) {
            if (!isShip(a)) continue;
            if (alreadyReady(a)){
                //log?.info("     HERE: got already has hullmod")
                var data: SCData? = SCControllerHullmod.getFleetData(a);
                if (data == null) continue
                map.put(a.originalOwner,data);
                addModules(a,data);
                //log?.info("     HERE: finished already has hullmod")
            }
        }
        Global.getCombatEngine().customData.put("SiC_SCDataMap",map);
    }
    override fun reportFleetMemberDeployed(member: DeployedFleetMemberAPI?) {
        val log: Logger? = Global.getLogger(SCControllerHullmod::class.java)
        var a = member?.ship;
        //log?.info(" HERE: started with name, id, hull: "+a?.name+", "+a?.id+", "+a?.hullSpec?.hullId)
        if (a == null || !isShip(a)) return
        //log?.info("     HERE: got valid ship")
        if (alreadyReady(a)){
            //log?.info("     HERE: got already has hullmod")
            var map: HashMap<Int?, SCData?> = Global.getCombatEngine().customData.get("SiC_SCDataMap") as HashMap<Int?, SCData?>;
            var data: SCData? = SCControllerHullmod.getFleetData(a);
            if (data == null) return
            map.put(a.originalOwner,data);
            Global.getCombatEngine().customData.put("SiC_SCDataMap",map);
            addModules(a,data);
            //log?.info("     HERE: finished already has hullmod")
        }else if (isValidShipToConvert(a)){
            //log?.info("     HERE: got need to add hullmod")
            var map: HashMap<Int?, SCData?> = Global.getCombatEngine().customData.get("SiC_SCDataMap") as HashMap<Int?, SCData?>;
            val force = a.originalOwner
            var data: SCData? = null;
            if (map.containsKey(force)) data = map.get(force);
            if (data == null) return
            refitShip(a,data);
            addModules(a,data);
            //log?.info("     HERE: finished need to add hullmod")
        }
        //log?.info(" isValidShipToConvert: is valid ship");
        //log?.info(" isValidShipToConvert: got data from other source?"+(data != null));
        //log?.info(" isValidShipToConvert: has item in map. is data null: "+(map.get(force)==null));
    }
    private fun isValidShipToConvert(a: ShipAPI) : Boolean{
        if (a.variant.hasHullMod(noSkillTagHullmodID)) return false;
        if (a.variant.hasTag(noSkillTagHullmodID)) return false;
        return true;
    }
    private fun alreadyReady(shipAPI: ShipAPI) : Boolean{
        return shipAPI.variant.hasHullMod("sc_skill_controller") && (shipAPI.customData.contains(SCControllerHullmod.secOverrideKey) || shipAPI.fleetMember.fleetData != null);
    }
    private fun isShip(shipAPI: ShipAPI) : Boolean{
        return !shipAPI.isHulk && shipAPI.isAlive && shipAPI.hullSize != ShipAPI.HullSize.FIGHTER && !shipAPI.isStationModule && shipAPI.parentStation == null
    }
    private fun refitShip(shipAPI: ShipAPI, data: SCData?){
        if (data == null) return
        SCControllerHullmod.addHullmodAfterShipCreation(shipAPI, data);
    }
    private fun addModules(shipAPI: ShipAPI, data: SCData?){
        if (!addModules) return
        val childs = ArrayList<ShipAPI?>()
        childs.addAll(shipAPI.childModulesCopy)
        var b = 0;
        while (b != childs.size){
            var a = childs[b]
            var aLinks = a?.childModulesCopy;
            if (aLinks != null) {
                for (c in aLinks){
                    if (childs.contains(c)) continue
                    childs.add(c);
                }
            }
            if (a != null && !alreadyReady(a) && isValidShipToConvert(a)) refitShip(a,data);
            b++;
        }
    }
}
