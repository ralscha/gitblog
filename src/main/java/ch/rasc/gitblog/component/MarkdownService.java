package ch.rasc.gitblog.component;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import com.vladsch.flexmark.ast.util.TextCollectingVisitor;
import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.ins.InsExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.superscript.SuperscriptExtension;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import ch.rasc.gitblog.util.BlankAnchorLinkExtension;

@Component
public class MarkdownService {

	private final Parser parser;

	private final HtmlRenderer renderer;

	public MarkdownService() {
		MutableDataSet options = new MutableDataSet();

		options.set(Parser.EXTENSIONS,
				Arrays.asList(AutolinkExtension.create(), AnchorLinkExtension.create(),
						TablesExtension.create(), AbbreviationExtension.create(),
						InsExtension.create(), SuperscriptExtension.create(),
						EmojiExtension.create(), DefinitionExtension.create(),
						FootnoteExtension.create(), BlankAnchorLinkExtension.create()));

		this.parser = Parser.builder(options).build();
		this.renderer = HtmlRenderer.builder(options).build();
	}

	public String renderHtml(String markdown) {
		Node document = this.parser.parse(markdown);
		return this.renderer.render(document);
	}

	public String renderText(String markdown) {
		Node document = this.parser.parse(markdown);
		TextCollectingVisitor textCollectingVisitor = new TextCollectingVisitor();
		return textCollectingVisitor.collectAndGetText(document);
	}
}
