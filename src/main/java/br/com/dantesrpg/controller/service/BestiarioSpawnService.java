package br.com.dantesrpg.controller.service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	/**
	 * Substitui os dados de combate de um inimigo morto pela variante maldita
	 * correspondente do bestiário, mantendo a mesma instância no combate.
	 */
	public boolean aplicarVersaoMaldita(Personagem monstro) {
		if (monstro == null) {
			return false;
		}

		Map<String, Object> dadosMalditos = localizarVersaoMaldita(monstro);
		if (dadosMalditos == null) {
			return false;
		}

		String nomeMaldito = (String) dadosMalditos.getOrDefault("nome", monstro.getNome() + " Maldito");
		monstro.setNome(aplicarSufixoDeInstancia(monstro.getNome(), nomeMaldito));
		monstro.setNomeBaseImagem((String) dadosMalditos.getOrDefault("nomeBaseImagem", nomeMaldito));

		double vida = ((Number) dadosMalditos.getOrDefault("vida", monstro.getVidaMaxima())).doubleValue();
		double mana = ((Number) dadosMalditos.getOrDefault("mana", 0.0)).doubleValue();
		int agilidade = lerAgilidade(dadosMalditos);
		int defesa = ((Number) dadosMalditos.getOrDefault("defesa", 0.0)).intValue();

		if (monstro.getAtributosBase() != null) {
			monstro.getAtributosBase().put(Atributo.DESTREZA, agilidade);
		}
		monstro.setVidaMaximaBase(vida);
		monstro.setArmaduraNatural(ArmaduraUtils.calcularPontosParaReducaoPercentual(defesa));
		monstro.setXpReward(((Number) dadosMalditos.getOrDefault("xpReward", monstro.getXpReward())).intValue());
		monstro.setGrau(((Number) dadosMalditos.getOrDefault("grau", 0.0)).intValue());
		monstro.setSegmentosVida(((Number) dadosMalditos.getOrDefault("segmentos", 0.0)).intValue());
		monstro.setAndar((String) dadosMalditos.getOrDefault("andar", "Não informado"));
		monstro.setPesoEntidade(PesoEntidade.fromJsonId((String) dadosMalditos.getOrDefault("peso", "medio_padrao")));
		monstro.setTamanhoX(((Number) dadosMalditos.getOrDefault("tamanhoX", 1.0)).intValue());
		monstro.setTamanhoY(((Number) dadosMalditos.getOrDefault("tamanhoY", 1.0)).intValue());
		List<String> propriedadesMalditas = lerPropriedades(dadosMalditos);
		propriedadesMalditas.removeIf(prop -> "MALDICAO_AO_MORRER".equalsIgnoreCase(prop));
		if (propriedadesMalditas.stream().noneMatch(prop -> "MALDITO".equalsIgnoreCase(prop))) {
			propriedadesMalditas.add("MALDITO");
		}
		monstro.setPropriedades(propriedadesMalditas);
		monstro.setPoderoso(Boolean.TRUE.equals(dadosMalditos.get("poderoso")));

		equiparArma(monstro, (String) dadosMalditos.getOrDefault("arma", null));
		monstro.recalcularAtributosEstatisticas();
		monstro.setVidaAtual(monstro.getVidaMaxima());
		monstro.setManaMaxima(mana);
		monstro.setManaAtual(mana);
		aplicarEscudosDePropriedade(monstro, vida);
		return true;
	}

	private Map<String, Object> localizarVersaoMaldita(Personagem monstro) {
		if (bestiarioDatabase == null || bestiarioDatabase.isEmpty()) {
			return null;
		}

		// O nome exibido pode receber um sufixo de instância (" 1", " 2") e
		// alguns registros usam uma chave técnica diferente do campo "nome".
		// Compare também a forma normalizada para não perder variantes por
		// diferenças de acentuação, caixa ou espaços.
		List<String> nomesBase = new ArrayList<>();
		adicionarNomeBase(nomesBase, monstro.getNomeBaseImagem());
		adicionarNomeBase(nomesBase, monstro.getNome());

		for (Map.Entry<String, Map<String, Object>> entrada : bestiarioDatabase.entrySet()) {
			Map<String, Object> dados = entrada.getValue();
			if (dados == null) {
				continue;
			}

			String nomeVariante = String.valueOf(dados.getOrDefault("nome", entrada.getKey()));
			String chaveVariante = entrada.getKey();
			boolean varianteMarcada = contemPropriedade(dados, "MALDITO")
					|| normalizarNome(nomeVariante).endsWith(" maldito")
					|| normalizarNome(chaveVariante).endsWith(" maldito");
			if (!varianteMarcada) {
				continue;
			}
			String baseDaVariante = removerSufixoMaldito(nomeVariante);
			String baseDaChave = removerSufixoMaldito(chaveVariante);
			for (String nomeBase : nomesBase) {
				String normalizado = normalizarNome(nomeBase);
				if (normalizado.equals(normalizarNome(baseDaVariante))
						|| normalizado.equals(normalizarNome(baseDaChave))) {
					return dados;
				}
			}
		}
		return null;
	}

	private void adicionarNomeBase(List<String> nomesBase, String nome) {
		if (nome == null || nome.isBlank()) {
			return;
		}
		String semInstancia = nome.trim().replaceFirst("\\s+\\d+$", "");
		if (!semInstancia.isBlank() && !nomesBase.contains(semInstancia)) {
			nomesBase.add(semInstancia);
		}
	}

	private String removerSufixoMaldito(String nome) {
		if (nome == null) {
			return "";
		}
		return nome.trim().replaceFirst("(?i)\\s+maldito$", "").trim();
	}

	private String normalizarNome(String nome) {
		String semAcentos = Normalizer.normalize(nome == null ? "" : nome, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		return semAcentos.replaceAll("\\s+", " ").trim().toLowerCase(java.util.Locale.ROOT);
	}

	private boolean contemPropriedade(Map<String, Object> dados, String propriedade) {
		Object valor = dados.get("propriedades");
		return valor instanceof List<?> lista
				&& lista.stream().anyMatch(item -> propriedade.equalsIgnoreCase(String.valueOf(item).trim()));
	}

	private String aplicarSufixoDeInstancia(String nomeAtual, String nomeBaseMaldito) {
		Matcher matcher = Pattern.compile("\\s+(\\d+)$").matcher(nomeAtual == null ? "" : nomeAtual);
		return matcher.find() ? nomeBaseMaldito + " " + matcher.group(1) : nomeBaseMaldito;
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
		String andar = (String) data.getOrDefault("andar", "Não informado");

		Map<Atributo, Integer> atributos = atributosBase(agilidade);
		long qtdExistente = estado.getCombatentes().stream().filter(p -> p.getNome().startsWith(nomeBase)).count();
		String nomeFinal = nomeBase + " " + (qtdExistente + 1);

		String racaStr = (String) data.getOrDefault("raca", "Criatura");
		Personagem monstro = new Personagem(nomeFinal, new RaçaPlaceholder(racaStr), new ClassePlaceholder(), 1, atributos,
				vidaMax, 0);
		monstro.setArmaduraNatural(ArmaduraUtils.calcularPontosParaReducaoPercentual(defesa));
		monstro.setFaccao("INIMIGO");
		monstro.setAndar(andar);
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
		String andar = (String) dadosMonstro.getOrDefault("andar", "Não informado");

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
		monstro.setAndar(andar);
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
