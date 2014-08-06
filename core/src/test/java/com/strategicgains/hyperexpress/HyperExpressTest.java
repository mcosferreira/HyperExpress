package com.strategicgains.hyperexpress;

import static com.strategicgains.hyperexpress.RelTypes.NEXT;
import static com.strategicgains.hyperexpress.RelTypes.PREV;
import static com.strategicgains.hyperexpress.RelTypes.SELF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.strategicgains.hyperexpress.domain.Blog;
import com.strategicgains.hyperexpress.domain.Entry;
import com.strategicgains.hyperexpress.domain.Link;
import com.strategicgains.hyperexpress.domain.Namespace;
import com.strategicgains.hyperexpress.domain.Resource;

public class HyperExpressTest
{
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		HyperExpress.registerResourceFactoryStrategy(new NullResourceFactoryStrategy(), "*");
		HyperExpress.relationships()
		.addNamespaces(
			new Namespace("ea", "http://namespaces.example.com/{rel}"),
			new Namespace("blog", "http://namespaces.example.com/{rel}")
		)

		.forCollectionOf(Blog.class)
			.rels(SELF, "/blogs")
				.withQuery("limit={selfLimit}")
				.withQuery("offset={selfOffset}")
			.rel(NEXT, "/blogs?limit={nextLimit}&offset={nextOffset}").optional()
			.rel(PREV, "/blogs?limit={prevLimit}&offset={prevOffset}").optional()

		.forClass(Entry.class)
			.rel(SELF, "/entries/{entryId}")
			.rel("edit", "/entries/{entryId}/edit")
				.attribute("method", "PUT")
				.optional("{adminRole}");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
	}

	@After
	public void cleanup()
	{
		HyperExpress.clearTokenBindings();
	}

	@Test
	public void shouldIgnoreOptionalLinks()
	{
		Resource r = HyperExpress.createCollectionResource(Collections.EMPTY_LIST, Blog.class, "*");
		assertNotNull(r.getLinks());
		assertEquals(1, r.getLinks().size());
		Link link = r.getLinks().iterator().next();
		assertEquals(SELF, link.getRel());
		assertTrue(HyperExpress.relationships().isCollectionArrayRel(Blog.class, link.getRel()));
		assertFalse(HyperExpress.relationships().isArrayRel(Blog.class, link.getRel()));
		assertEquals("/blogs", link.getHref());

		r = HyperExpress.createResource(new Entry(), "*");
		assertNotNull(r.getLinks());
		assertEquals(1, r.getLinks().size());
		link = r.getLinks().iterator().next();
		assertEquals(SELF, link.getRel());
		assertFalse(HyperExpress.relationships().isArrayRel(Entry.class, link.getRel()));
		assertFalse(HyperExpress.relationships().isCollectionArrayRel(Entry.class, link.getRel()));
		assertEquals("/entries/{entryId}", link.getHref());		

		HyperExpress.bind("adminRole", "false");
		r = HyperExpress.createResource(new Entry(), "*");
		assertNotNull(r.getLinks());
		assertEquals(1, r.getLinks().size());
		link = r.getLinks().iterator().next();
		assertEquals(SELF, link.getRel());
		assertFalse(HyperExpress.relationships().isArrayRel(Entry.class, link.getRel()));
		assertFalse(HyperExpress.relationships().isCollectionArrayRel(Entry.class, link.getRel()));
		assertEquals("/entries/{entryId}", link.getHref());		
	}

	@Test
	public void shouldContainOptionalQueryStringParameters()
	{
		HyperExpress
			.bind("selfOffset", "40")
			.bind("selfLimit", "20");
		Resource r = HyperExpress.createCollectionResource(null, Blog.class, "*");
		assertNotNull(r.getLinks());
		assertEquals(1, r.getLinks().size());
		Link link = r.getLinks().iterator().next();
		assertEquals(SELF, link.getRel());
		assertEquals("/blogs?limit=20&offset=40", link.getHref());
		HyperExpress.clearTokenBindings();
	}

	@Test
	public void shouldContainOptionalLinks()
	{
		HyperExpress
			.bind("nextOffset", "40")
			.bind("nextLimit", "20");
		Resource r = HyperExpress.createCollectionResource(null, Blog.class, "*");
		assertNotNull(r.getLinks());
		assertEquals(2, r.getLinks().size());
		Iterator<Link> iter = r.getLinks().iterator();

		while(iter.hasNext())
		{
			Link link = iter.next();

			switch (link.getRel())
            {
				case SELF:
					assertEquals("/blogs", link.getHref());
					break;

				case NEXT:
					assertEquals("/blogs?limit=20&offset=40", link.getHref());
					break;

				default:
					fail("Unexpected 'rel' " + link.getRel());
					break;
			}
		}
	}

	@Test
	public void shouldContainOptionallyBoundLinksWithBoolean()
	{
		HyperExpress.bind("adminRole", Boolean.TRUE.toString());
		Resource r = HyperExpress.createResource(new Entry(), "*");
		assertNotNull(r.getLinks());
		assertEquals(2, r.getLinks().size());
		Iterator<Link> iter = r.getLinks().iterator();

		while(iter.hasNext())
		{
			Link link = iter.next();

			switch (link.getRel())
            {
				case SELF:
					assertEquals("/entries/{entryId}", link.getHref());
					break;

				case "edit":
					assertEquals("/entries/{entryId}/edit", link.getHref());
					break;

				default:
					fail("Unexpected 'rel' " + link.getRel());
					break;
			}
		}
	}

	@Test
	public void shouldContainOptionallyBoundLinksWithString()
	{
		HyperExpress.bind("adminRole", "admin");
		Resource r = HyperExpress.createResource(new Entry(), "*");
		assertNotNull(r.getLinks());
		assertEquals(2, r.getLinks().size());
		Iterator<Link> iter = r.getLinks().iterator();

		while(iter.hasNext())
		{
			Link link = iter.next();

			switch (link.getRel())
            {
				case SELF:
					assertEquals("/entries/{entryId}", link.getHref());
					break;

				case "edit":
					assertEquals("/entries/{entryId}/edit", link.getHref());
					break;

				default:
					fail("Unexpected 'rel' " + link.getRel());
					break;
			}
		}
	}

	@Test
	public void shouldHandleCollection()
	{
		ArrayList<Blog> blogs = new ArrayList<>();
		Resource r = HyperExpress.createCollectionResource(blogs, Blog.class, "*");
		assertNotNull(r);
	}

	@Test
	public void shouldCreateEmptyResourceFromResource()
	{
		Resource r = HyperExpress.createResource(new AgnosticResource(), "*");
		assertEmptyResource(r);
	}

	@Test
	public void shouldCreateCollectionResourceFromResourceCollection()
	{
		Collection<Resource> resources = new ArrayList<Resource>();
		resources.add(new AgnosticResource());
		Resource r = HyperExpress.createCollectionResource(resources, Blog.class, "blogs", "*");
		assertNotNull(r);
		assertTrue(r.hasNamespaces());
		assertEquals(2, r.getNamespaces().size());
		assertTrue(r.hasLinks());
		assertEquals(1, r.getLinks().size());
		assertEquals("self", r.getLinks().get(0).getRel());
		assertEquals("/blogs", r.getLinks().get(0).getHref());
		assertFalse(r.hasProperties());
		assertTrue(r.hasResources());
		assertEquals(1, r.getResources("blogs").size());
		assertEmptyResource(r.getResources("blogs").get(0));
	}

	private void assertEmptyResource(Resource r)
	{
		assertNotNull(r);
		assertTrue(r.hasNamespaces());
		assertEquals(2, r.getNamespaces().size());
		assertFalse(r.hasLinks());
		assertFalse(r.hasProperties());
		assertFalse(r.hasResources());
	}
}
