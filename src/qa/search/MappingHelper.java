package qa.search;

import java.util.HashMap;
import qa.model.enumerator.QuerySubType;;

public class MappingHelper {
	
	HashMap<QuerySubType, String> map;
	
	public MappingHelper(){
		map = new HashMap<QuerySubType, String>();
		Map();
	}
	
	public void Map(){
		
		map.put(QuerySubType.ABBR_abb, ""); //remember to remove "acronym that comes with search
		map.put(QuerySubType.ABBR_exp, "expand full");
		map.put(QuerySubType.DESC_def, "define");
		map.put(QuerySubType.DESC_desc, "what");
		map.put(QuerySubType.DESC_manner, "");
		map.put(QuerySubType.DESC_reason, "because due to");
		map.put(QuerySubType.ENTY_animal,"animal");
		map.put(QuerySubType.ENTY_body,"body");
		map.put(QuerySubType.ENTY_color,"color");
		map.put(QuerySubType.ENTY_cremat,"");
		map.put(QuerySubType.ENTY_currency,"");
		map.put(QuerySubType.ENTY_dismed,"");
		map.put(QuerySubType.ENTY_event,"");
		map.put(QuerySubType.ENTY_food,"eat");
		map.put(QuerySubType.ENTY_instru,"music device");
		map.put(QuerySubType.ENTY_lang,"speak");
		map.put(QuerySubType.ENTY_letter,"");
		map.put(QuerySubType.ENTY_other,"");
		map.put(QuerySubType.ENTY_plant,"grow");
		map.put(QuerySubType.ENTY_product,"");
		map.put(QuerySubType.ENTY_religion,"religion god pray believe");
		map.put(QuerySubType.ENTY_sport,"sport play ball");
		map.put(QuerySubType.ENTY_substance,"substance  ");
		map.put(QuerySubType.ENTY_symbol,"symbol sign");
		map.put(QuerySubType.ENTY_techmeth,"");
		map.put(QuerySubType.ENTY_termeq,"");
		map.put(QuerySubType.ENTY_veh,"");
		map.put(QuerySubType.ENTY_word,"");
		map.put(QuerySubType.HUM_desc,"");
		map.put(QuerySubType.HUM_gr,"group team");
		map.put(QuerySubType.HUM_ind,"");
		map.put(QuerySubType.HUM_title,"title position ");
		map.put(QuerySubType.LOC_city,"city");
		map.put(QuerySubType.LOC_country,"country");
		map.put(QuerySubType.LOC_mount,"mountain");
		map.put(QuerySubType.LOC_other,"place");
		map.put(QuerySubType.LOC_state,"state");
		map.put(QuerySubType.NUM_code,"");
		map.put(QuerySubType.NUM_count,"amount quantity");
		map.put(QuerySubType.NUM_date,"date");
		map.put(QuerySubType.NUM_dist,"");
		map.put(QuerySubType.NUM_money,"cost amount");
		map.put(QuerySubType.NUM_ord,"order position");
		map.put(QuerySubType.NUM_other,"");
		map.put(QuerySubType.NUM_period,"during timespan");
		map.put(QuerySubType.NUM_perc,"percent ");
		map.put(QuerySubType.NUM_speed,"speed how fast");
		map.put(QuerySubType.NUM_temp,"temperature hot cold");
		map.put(QuerySubType.NUM_volsize,"volume size amount");
		map.put(QuerySubType.NUM_weight,"weight how heavy light");
		
	}
	
	public HashMap<QuerySubType, String> getMap(){
		return this.map;
	}
}
