package br.com.dantesrpg.model.util;

import br.com.dantesrpg.model.Habilidade;
import br.com.dantesrpg.model.habilidades.*;
import br.com.dantesrpg.model.habilidades.boss.*;
import br.com.dantesrpg.model.habilidades.classe.*;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

public class HabilidadeFactory {

	// Mapa que guarda "Nome da Habilidade" -> "Como criar ela"
	private static final Map<String, Supplier<Habilidade>> registro = new TreeMap<>();

	static {
		// Genéricas / Armas
		registro.put("Iaijutsu", Iaijutsu::new);
		registro.put("Passo Sombrio", PassoSombrio::new);
		registro.put("Rajada de Drone", RajadaDrone::new);
		registro.put("Rajada de Drone II", RajadaDrone2::new);
		registro.put("Investida Flamejante", InvestidaDeChamas::new);
		registro.put("Nano-Guarda", NanoGuarda::new);
		registro.put("Campo de Força", CampoDeForça::new);
		registro.put("Campo de Força II", CampoDeForça::new);
		registro.put("Cura I", Cura_Tier1::new);
		registro.put("Bombardeio", Bombardeio::new);
		registro.put("Investida Serra-Espada", InvestidaDeSerraEspada::new);
		registro.put("Rodar Serra-Espada", RodarDeSerraEspada::new);
		registro.put("Arremesso Serra-Espada", ArremessoDeSerraEspada::new);
		registro.put("DoubleDown", DoubleDown::new);
		registro.put("Arremesso de Orb", ArremessoDeOrb::new);
		registro.put("Pisotear", PisarOrb::new);
		registro.put("Bola de Fogo", BolaDeFogo::new);
		registro.put("Barragem de Espadas", LaminasDaJustiça::new);
		registro.put("Cura Suprema", CuraSuprema::new);
		registro.put("Mordida", Mordida::new);
		registro.put("Tecnica de Amplificação", TecnicaDeAmplificação::new);
		registro.put("MordidaQuente", MordidaQuente::new);
		registro.put("SLAM", SLAM::new);
		registro.put("SLAP", SLAP::new);
		registro.put("HOOK", HOOK::new);
		registro.put("Roubo de Essência", RouboDeEssencia::new);
		registro.put("Ceifa Ampla", DescargaEletrica::new);
		registro.put("Presença do Zero", PresencaDoZero::new);
		registro.put("Invocar Tentáculo", InvocarImp::new);
		registro.put("Puxar Essencia", PuxarEssencia::new);
		registro.put("Carga Dimensional", CargaDimensional::new);
		registro.put("Lâminas Espectrais", LaminasEspectrais::new);
		registro.put("Lightning Strike", LightningStrike::new);
		registro.put("Corte Temporal", CorteTemporal::new);
		registro.put("Arremesso de Machado", ArremessoDeMachad::new);
		registro.put("Sex in the air", SexInTheAir::new);
		registro.put("True love!", TrueLove::new);
		registro.put("Eternal Love!", EternalLove::new);
		registro.put("Invocar Imp", InvocarImp::new);
		registro.put("Projetil Explosivo", ProjetilExplosivo::new);
		registro.put("Pulo Pesado", PularStompLuxuria::new);
		registro.put("Chamar Reforços", ChamarReforços::new);
		registro.put("Descarga eletrica", DescargaEletrica::new);
		registro.put("Dilacerar", Dilacerar::new);
		registro.put("Invocar Filth de Sangue", InvocarFilthDeSangue::new);
		registro.put("Coagular", Coagular::new);
		registro.put("Arremesso", Arremesso::new);
		registro.put("Enraizar", Enraizar::new);
		registro.put("Cardial", Cardial::new);
		registro.put("Destructa", Destructa::new);
		registro.put("Sanctum", Sanctum::new);
		registro.put("Purus", Purus::new);
		registro.put("Locus", Locus::new);
		registro.put("MEAT...", Meat::new);
		registro.put("Sentinela", SentinelaSkill::new);
		registro.put("Hide and Build", HideAndBuild::new);
		registro.put("Poison Was Lethal", PoisonWasLethal::new);
		registro.put("Sit in Balance", SitInBalance::new);
		registro.put("Sacrifício Sangrento", SacrificioSangrento::new);
		registro.put("Parada Cardíaca", ParadaCardiaca::new);
		registro.put("Respirar", Respirar::new);
		registro.put("Estocada Divina", EstocadaDivina::new);
		registro.put("Corte Divino", CorteDivino::new);
		registro.put("Quebra-Elmo", QuebraElmo::new);
		registro.put("Arremesso Divino", ArremessoDivino::new);
		registro.put("pisarOrb", PisarOrb::new);
		registro.put("Corte de Punição", CorteDePunicao::new);
		registro.put("Recarga de Areia", RecargaDeAreia::new);
		registro.put("Corte de Apófis", CorteDeApofis::new);
		registro.put("Véu de Sombras", VeuDeSombras::new);
		registro.put("Bênção de Maat", BencaoDeMaat::new);
		registro.put("Sacrifício de Maat", SacrificioDeMaat::new);

		// Lua Profana
		registro.put("sweet dreams", SweetDreams::new);
		registro.put("bitter Hunt", BitterHunt::new);
		registro.put("moonshine", Moonshine::new);

		// Elion
		registro.put("lighting thunder", LightingThunder::new);
		registro.put("lighting vegeance", LightingVengeance::new);

		// Classes
		registro.put("Técnica de Barreira", TecnicaDeBarreira::new);
		registro.put("Reversão de Feitiço", ReversaoDeFeitico::new);
		registro.put("Purificar", Purificar::new);
		registro.put("Bênção", Bencao::new);
		registro.put("Trocado", Trocado::new);
		registro.put("DeadEye", DeadEye::new);
		registro.put("Gatilho Veloz", GatilhoVeloz::new);
		registro.put("Descarregar Tambor", DescarregarTambor::new);
		registro.put("Justiça Dourada", JusticaDourada::new);
		registro.put("Combo!", Combo::new);
		registro.put("Ilusão", Ilusao::new);
		registro.put("Rezar", Rezar::new);
		registro.put("Auto-Guarda", AutoGuarda::new);
		registro.put("Invocar Golem", InvocarGolem::new);
		registro.put("Invocar Guardião", InvocarGuardiao::new);
		registro.put("Bash Strike", BashStrike::new);
		registro.put("Impacto Vingativo", ImpactoVingativo::new);
		registro.put("Drenagem de Efeitos", DrenagemDeEfeitos::new);
		registro.put("Raiva Imparável", RaivaImparavel::new);
		registro.put("Balanço Temerário", BalancoTemerario::new);
		registro.put("Grito de Guerra", GritoDeGuerra::new);
		registro.put("Golpe Devastador", GolpeDevastador::new);
		registro.put("SANGUE...", Sangue::new);
		registro.put("Fome...", Fome::new);

		// KuangLi - Profeta de Behemoth
		registro.put("Mergulho", Mergulho::new);
		registro.put("Pedregulho", Pedregulho::new);
		registro.put("Fortificar", Fortificar::new);
		registro.put("Paredão de Pedra", ParedaoDePedra::new);
		registro.put("Meteoro", Meteoro::new);
		registro.put("Terremoto", Terremoto::new);
	}

	public static Habilidade criarHabilidadePorNome(String nome) {
		if (nome == null || !registro.containsKey(nome)) {
			System.err.println("Factory: Habilidade '" + nome + "' não encontrada.");
			return null;
		}
		return registro.get(nome).get();
	}

	public static Set<String> getNomesDisponiveis() {
		return registro.keySet();
	}
}
