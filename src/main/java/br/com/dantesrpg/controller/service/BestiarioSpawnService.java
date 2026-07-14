package br.com.dantesrpg.controller.service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import br.com.dantesrpg.controller.CombatController;
import br.com.dantesrpg.model.Arma;
import br.com.dantesrpg.model.EstadoCombate;
import br.com.dantesrpg.model.Personagem;
import br.com.dantesrpg.model.classes.ClassePlaceholder;
import br.com.dantesrpg.model.enums.Atributo;
import br.com.dantesrpg.model.enums.PesoEntidade;
import br.com.dantesrpg.model.racas.RaçaPlaceholder;
import br.com.dantesrpg.model.util.ArmaduraUtils;
import br.com.dantesrpg.model.util.FileLoader;

public class BestiarioSpawnService {

	private final CombatController controller;
	private final CatalogoItensService catalogoItensService;
	private final Supplier<EstadoCombate> estadoSupplier;

	private Map<String, Map<String, Object>> bestiarioDatabase = new HashMap<>();
	private Map<String, Object> templateSpawnCustomizado;
	private boolean modoSpawnAtivo;
	private String idMonstroParaSpawn;

	public BestiarioSpawnService(CombatController controller, CatalogoItensService catalogoItensService,
			Supplier<EstadoCombate> estadoSupplier) {
		this.controller = controller;
		this.catalogoItensService = catalogoItensService;
		this.estadoSupplier = estadoSupplier;
	}

	public void carregarBestiario() {
		this.bestiarioDatabase = new HashMap<>();
		Gson gson = new Gson();
		String resourcePath = "/data/bestiario.json";
		try (InputStream is = FileLoader.carregarArquivo(resourcePath)) {
			if (is == null) {
				return;
			}
			try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
				Type mapType = new TypeToken<Map<String, Map<String, Object>>>() {
				}.getType();
				this.bestiarioDatabase = gson.fromJson(reader, mapType);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Erro ao ler bestiario.json:");
		}
	}

	public Map<String, Map<String, Object>> getBestiarioDatabase() {
		return bestiarioDatabase;
	}

	public void entrarModoSpawnMultiploCustom(Map<String, Object> dadosCustom, int quantidade) {
		this.templateSpawnCustomizado = dadosCustom;
		this.modoSpawnAtivo = true;
		this.idMonstroParaSpawn = (String) dadosCustom.getOrDefault("id", "Custom");

		final int qty = quantidade;
		controller.forEachMap(m -> m.entrarModoSpawn(idMonstroParaSpawn, qty));
	}

	public void entrarModoSpawn(String idMonstro) {
		this.idMonstroParaSpawn = idMonstro;
		this.modoSpawnAtivo = true;

		controller.forEachMap(m -> m.entrarModoSpawn(idMonstro, 1));
		System.out.println("GM: Modo SPAWN ATIVO para: " + idMonstro);
	}

	public void entrarModoSpawnMultiplo(String idMonstro, int quantidade) {
		this.idMonstroParaSpawn = idMonstro;
		this.modoSpawnAtivo = true;

		controller.forEachMap(m -> m.entrarModoSpawn(idMonstro, quantidade));
	}

	public void notifySpawnConcluido() {
		this.modoSpawnAtivo = false;
		this.idMonstroParaSpawn = null;
		this.templateSpawnCustomizado = null;
		controller.forEachMap(m -> m.cancelarModoSpawn());
		System.out.println("GM: Modo SPAWN finalizado.");
	}

	public void spawnarMonstro(String idMonstro, int x, int y) {
		EstadoCombate estado = getEstado();
		if (estado == null) {
			return;
		}
		if (bestiarioDatabase == null || !bestiarioDatabase.containsKey(idMonstro)) {
			System.err.println("Erro: Monstro '" + idMonstro + "' não encontrado no bestiário.");
			return;
		}

		Map<String, Object> data = bestiarioDatabase.get(idMonstro);
		String nomeBase = (String) data.getOrDefault("nome", idMonstro);
		String nomeBaseImagem = (String) data.getOrDefault("nomeBaseImagem", nomeBase);
		int vidaMax = ((Double) data.getOrDefault("vida", 10.0)).intValue();
		int agilidade = ((Double) data.getOrDefault("agilidade", 1.0)).intValue();
		int defesa = ((Double) data.getOrDefault("defesa", 0.0)).intValue();
		String nomeArma = (String) data.getOrDefault("arma", null);
		int xpReward = ((Double) data.getOrDefault("xpReward", 0.0)).intValue();
		int tamanhoX = data.containsKey("tamanhoX") ? ((Number) data.get("tamanhoX")).intValue() : 1;
		int tamanhoY = data.containsKey("tamanhoY") ? ((Number) data.get("tamanhoY")).intValue() : 1;
		int segmentos = ((Number) data.getOrDefault("segmentos", 0.0)).intValue();
		int grau = ((Number) data.getOrDefault("grau", 0.0)).intValue();
		String pesoStr = (String) data.getOrDefault("peso", "medio_padrao");

		Map<Atributo, Integer> atributos = atributosBase(agilidade);
		long qtdExistente = estado.getCombatentes().stream().filter(p -> p.getNome().startsWith(nomeBase)).count();
		String nomeFinal = nomeBase + " " + (qtdExistente + 1);

		String racaStr = (String) data.getOrDefault("raca", "Criatura");
		Personagem monstro = new Personagem(nomeFinal, new RaçaPlaceholder(racaStr), new ClassePlaceholder(), 1, atributos,
				vidaMax, 0);
		monstro.setArmaduraNatural(ArmaduraUtils.calcularPontosParaReducaoPercentual(defesa));
		monstro.setFaccao("INIMIGO");
		monstro.setNomeBaseImagem(nomeBaseImagem);
		monstro.setXpReward(xpReward);
		monstro.setPosX(x);
		monstro.setPosY(y);
		monstro.setTamanhoX(tamanhoX);
		monstro.setTamanhoY(tamanhoY);

		if (idMonstro.equalsIgnoreCase("Nebrion")) {
			monstro.setTamanhoX(7);
			monstro.setTamanhoY(7);
		}

		aplicarFantasmaNobreMonstro(idMonstro, monstro);
		monstro.setGrau(grau);
		monstro.setSegmentosVida(segmentos);
		monstro.setPesoEntidade(PesoEntidade.fromJsonId(pesoStr));
		monstro.setPropriedades(lerPropriedades(data));
		
		boolean poderoso = data.containsKey("poderoso") && (Boolean) data.get("poderoso");
		monstro.setPoderoso(poderoso);
		
		equiparArma(monstro, nomeArma);
		aplicarEscudosDePropriedade(monstro, vidaMax);

		monstro.recalcularAtributosEstatisticas();
		monstro.setVidaAtual(monstro.getVidaMaxima());
		estado.getCombatentes().add(monstro);

		controller.atualizarInterfaceTotal();
		System.out.println("SPAWN: " + nomeFinal + " (Segmentos: " + segmentos + ")");
	}

	public void resolverSpawn(String idMonstro, int x, int y) {
		EstadoCombate estado = getEstado();
		if (!modoSpawnAtivo || estado == null) {
			return;
		}

		Map<String, Object> dadosMonstro = templateSpawnCustomizado != null
				? templateSpawnCustomizado
				: bestiarioDatabase.get(idMonstro);

		if (dadosMonstro == null) {
			System.err.println("Erro: Template de monstro não encontrado: " + idMonstro);
			return;
		}

		String nome = (String) dadosMonstro.getOrDefault("nome", idMonstro);
		String nomeBaseImagem = (String) dadosMonstro.getOrDefault("nomeBaseImagem", nome);
		double vida = ((Number) dadosMonstro.getOrDefault("vida", 10.0)).doubleValue();
		double mana = ((Number) dadosMonstro.getOrDefault("mana", 0.0)).doubleValue();
		int agilidade = lerAgilidade(dadosMonstro);
		int defesa = ((Number) dadosMonstro.getOrDefault("defesa", 0.0)).intValue();
		int tamanhoX = dadosMonstro.containsKey("tamanhoX") ? ((Number) dadosMonstro.get("tamanhoX")).intValue() : 1;
		int tamanhoY = dadosMonstro.containsKey("tamanhoY") ? ((Number) dadosMonstro.get("tamanhoY")).intValue() : 1;

		System.out.println("DEBUG JSON KEYS: " + dadosMonstro.keySet());
		int xp = ((Number) dadosMonstro.getOrDefault("xpReward", 0.0)).intValue();
		int grau = ((Number) dadosMonstro.getOrDefault("grau", 0.0)).intValue();
		int segmentos = ((Number) dadosMonstro.getOrDefault("segmentos", 0.0)).intValue();
		String nomeArma = (String) dadosMonstro.getOrDefault("arma", null);

		if (idMonstro.equalsIgnoreCase("Nebrion")) {
			tamanhoX = 7;
			tamanhoY = 7;
		}
		if (idMonstro.startsWith("MinosFase")) {
			tamanhoX = 24;
			tamanhoY = 24;
		}

		List<String> props = lerPropriedades(dadosMonstro);
		List<Personagem> existentes = estado.getCombatentes().stream()
				.filter(p -> p.getNome().startsWith(nome))
				.collect(Collectors.toList());
		String nomeFinal = definirNomeSpawnado(nome, existentes);

		String racaStr = (String) dadosMonstro.getOrDefault("raca", "Criatura");
		Personagem monstro = new Personagem(nomeFinal, new RaçaPlaceholder(racaStr), new ClassePlaceholder(), 1,
				atributosBase(agilidade), vida, 0);
		monstro.setArmaduraNatural(ArmaduraUtils.calcularPontosParaReducaoPercentual(defesa));
		monstro.setVidaMaxima(vida);
		monstro.setVidaAtual(vida);
		monstro.setManaMaxima(mana);
		monstro.setManaAtual(mana);
		monstro.setXpReward(xp);
		monstro.setGrau(grau);
		monstro.setSegmentosVida(segmentos);
		System.out.println("DEBUG SPAWN: " + nomeFinal + " criado com " + segmentos + " segmentos.");

		monstro.setTamanhoX(tamanhoX);
		monstro.setTamanhoY(tamanhoY);
		monstro.setPosX(x);
		monstro.setPosY(y);
		monstro.setFaccao("INIMIGO");
		monstro.setNomeBaseImagem(nomeBaseImagem);
		monstro.setPropriedades(props);
		
		boolean poderoso = dadosMonstro.containsKey("poderoso") && (Boolean) dadosMonstro.get("poderoso");
		monstro.setPoderoso(poderoso);

		equiparArma(monstro, nomeArma);
		aplicarEscudosDePropriedade(monstro, vida);
		aplicarFantasmaNobreMonstro(idMonstro, monstro);

		monstro.recalcularAtributosEstatisticas();
		monstro.setVidaAtual(monstro.getVidaMaxima());
		estado.getCombatentes().add(monstro);

		controller.atualizarInterfaceTotal();
		System.out.println("GM: Monstro spawnado: " + nomeFinal + " [Custom: "
				+ (this.templateSpawnCustomizado != null) + "]");
	}

	private EstadoCombate getEstado() {
		return estadoSupplier.get();
	}

	private Map<Atributo, Integer> atributosBase(int agilidade) {
		Map<Atributo, Integer> atributos = new HashMap<>();
		for (Atributo atributo : Atributo.values()) {
			atributos.put(atributo, 1);
		}
		atributos.put(Atributo.DESTREZA, agilidade);
		return atributos;
	}

	private int lerAgilidade(Map<String, Object> dadosMonstro) {
		if (dadosMonstro.containsKey("agilidade")) {
			return ((Number) dadosMonstro.get("agilidade")).intValue();
		}
		if (dadosMonstro.containsKey("iniciativaBase")) {
			return ((Number) dadosMonstro.get("iniciativaBase")).intValue();
		}
		return 1;
	}

	private List<String> lerPropriedades(Map<String, Object> data) {
		List<String> props = new ArrayList<>();
		Object propsObj = data.getOrDefault("propriedades", null);
		if (propsObj instanceof List) {
			props.addAll((List<String>) propsObj);
		}
		return props;
	}

	private String definirNomeSpawnado(String nome, List<Personagem> existentes) {
		if (existentes.isEmpty()) {
			return nome;
		}

		Optional<Personagem> purista = existentes.stream().filter(p -> p.getNome().equals(nome)).findFirst();
		if (purista.isPresent()) {
			purista.get().setNome(nome + " 1");
			System.out.println("GM: " + nome + " original foi renomeado para " + nome + " 1");
		}

		return nome + " " + (existentes.size() + 1);
	}

	private void equiparArma(Personagem monstro, String nomeArma) {
		if (nomeArma != null && !nomeArma.isEmpty()) {
			Arma arma = catalogoItensService.getArma(nomeArma);
			if (arma != null) {
				monstro.setArmaEquipada(arma);
				return;
			}
		}
		monstro.setArmaEquipada(catalogoItensService.getArma("Punhos"));
	}

	private void aplicarEscudosDePropriedade(Personagem monstro, double vidaBase) {
		int nivelBlindado = monstro.getValorPropriedade("BLINDADO");
		if (nivelBlindado > 0) {
			double porcentagem = 0.20 * nivelBlindado;
			monstro.adicionarEscudoNormal(vidaBase * porcentagem);
			System.out.println(">>> PROPRIEDADE: Blindado Nível " + nivelBlindado + " ("
					+ (int) (porcentagem * 100) + "% Escudo)");
		}

		int nivelArmadurado = monstro.getValorPropriedade("ARMADURADO");
		if (nivelArmadurado > 0) {
			double porcentagem = 0.50 * nivelArmadurado;
			monstro.adicionarEscudoNormal(vidaBase * porcentagem);
		}
	}

	public static void aplicarFantasmaNobreMonstro(String idMonstro, Personagem monstro) {
		if (idMonstro.equalsIgnoreCase("morghul")) {
			monstro.setFantasmaNobre(new br.com.dantesrpg.model.fantasmasnobres.Ritual());
		}
		if (idMonstro.equalsIgnoreCase("lillith")) {
			monstro.setFantasmaNobre(new br.com.dantesrpg.model.fantasmasnobres.JihoGekkyuden());
		}
		if (idMonstro.equalsIgnoreCase("JusticeiroCego")) {
			monstro.setFantasmaNobre(new br.com.dantesrpg.model.fantasmasnobres.AndJusticeForMySelf());
		}
		if (idMonstro.equalsIgnoreCase("ArcanjoExecutor")) {
			monstro.setRaca(new br.com.dantesrpg.model.racas.Arcanjo());
			monstro.setFantasmaNobre(new br.com.dantesrpg.model.fantasmasnobres.GodsWill());
		}
		if (idMonstro.equalsIgnoreCase("Lua Profana")) {
			monstro.setFantasmaNobre(new br.com.dantesrpg.model.fantasmasnobres.LuaSombria());
		}
		if (idMonstro.equalsIgnoreCase("Eremita") || monstro.getNome().contains("RockFeller")) {
			monstro.setFantasmaNobre(new br.com.dantesrpg.model.fantasmasnobres.EspadaDeMilSois());
		}
		if (idMonstro.equalsIgnoreCase("Escanor")) {
			monstro.setFantasmaNobre(new br.com.dantesrpg.model.fantasmasnobres.InefavelSol());
		}
	}
}
