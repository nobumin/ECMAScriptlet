package info.dragonlady.filter;

/**
 * ログレベルを列挙型で定義
 * @author nobu
 *
 */
public enum LogLevel {
	none,	//ログ出力なし
	smart,	//最小限のログ出力
	middle,	//通常のログ出力（推奨）
	full,	//レスポンスを除くログ出力（ファイルアップロードを行なう場合は利用しない事）
	debug	//レスポンスを含むログ出力（大きなデータを出力する場合利用しない事）
}
