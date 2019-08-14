package ch.rasc.gitblog.util;

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.util.builder.Extension;
import com.vladsch.flexmark.util.data.MutableDataHolder;

public class BlankAnchorLinkExtension implements HtmlRenderer.HtmlRendererExtension {

	@Override
	public void rendererOptions(MutableDataHolder options) {
		// Currently no options supported.
	}

	@Override
	public void extend(final HtmlRenderer.Builder rendererBuilder,
			final String rendererType) {
		rendererBuilder
				.attributeProviderFactory(new IndependentAttributeProviderFactory() {
					@Override
					public AttributeProvider apply(LinkResolverContext context) {
						return (node, part, attributes) -> {
							if (node instanceof Link && part == AttributablePart.LINK) {
								attributes.replaceValue("target", "_blank");
							}
						};
					}
				});
	}

	/**
	 * Extensions are added by providing a class to flexmark. Flexmark then invokes the
	 * extension's static `create` method through reflection.
	 *
	 * @return an instance of the extension
	 */
	public static Extension create() {
		return new BlankAnchorLinkExtension();
	}
}