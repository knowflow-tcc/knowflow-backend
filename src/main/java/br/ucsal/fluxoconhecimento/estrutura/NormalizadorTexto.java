package br.ucsal.fluxoconhecimento.estrutura;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

// Transforma o texto em termos para o índice:
// tira acentos, junta "Wi-Fi" -> "wifi", remove stopwords e passa plural simples para o singular
public final class NormalizadorTexto {

    private static final Set<String> STOPWORDS = Set.of(
            "as", "os", "de", "da", "do", "das", "dos", "em", "no", "na", "nos", "nas",
            "um", "uma", "uns", "umas", "para", "pra", "pro", "por", "pelo", "pela", "pelos", "pelas",
            "com", "sem", "que", "se", "ao", "aos", "ou", "mas", "mais", "muito", "muita", "muitos",
            "muitas", "quando", "como", "onde", "nao", "sim", "ja", "esta", "estao", "estava", "foi",
            "ser", "ter", "tem", "sao", "seu", "sua", "seus", "suas", "meu", "minha", "eu", "ele",
            "ela", "eles", "elas", "isso", "isto", "este", "esse", "essa", "aquele", "aquela", "ate",
            "apos", "entre", "sobre", "tambem", "num", "numa", "lhe", "me", "todo", "toda", "todos",
            "todas", "vez", "vezes", "fica", "ficam", "ficou", "ficar", "sempre", "ainda", "so", "cada",
            "depois", "antes", "agora", "aqui", "ali", "la");

    private static final Pattern MARCAS_DE_ACENTO = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARADOR = Pattern.compile("[^a-z0-9]+");

    private NormalizadorTexto() {
    }

    // Termos do texto, na ordem em que aparecem (com repetições)
    public static List<String> termos(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        String limpo = MARCAS_DE_ACENTO
                .matcher(Normalizer.normalize(texto, Normalizer.Form.NFD))
                .replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replace("-", "");

        List<String> termos = new ArrayList<>();
        for (String palavra : SEPARADOR.split(limpo)) {
            if (palavra.length() < 2 || STOPWORDS.contains(palavra)) {
                continue;
            }
            String radical = reduzirPlural(palavra);
            if (!STOPWORDS.contains(radical)) {
                termos.add(radical);
            }
        }
        return termos;
    }

    static String reduzirPlural(String palavra) {
        int n = palavra.length();
        if (n > 4 && (palavra.endsWith("oes") || palavra.endsWith("aes"))) {
            return palavra.substring(0, n - 3) + "ao";
        }
        if (n > 3 && palavra.endsWith("ns")) {
            return palavra.substring(0, n - 2) + "m";
        }
        if (n > 3 && palavra.endsWith("s")
                && !palavra.endsWith("ss") && !palavra.endsWith("us") && !palavra.endsWith("is")) {
            return palavra.substring(0, n - 1);
        }
        return palavra;
    }
}
