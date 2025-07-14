package com.kalavit.javulna.services;

import com.kalavit.javulna.dto.MovieDto;
import com.kalavit.javulna.model.Movie;
import com.kalavit.javulna.services.autodao.MovieAutoDao;
import java.io.ByteArrayInputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.XMLConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Service
public class MovieService {

    private static final Logger LOG = LoggerFactory.getLogger(MovieService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MovieAutoDao movieAutoDao;

    public List<MovieDto> findMovie(String title, String description, String genre, String id) {
        StringBuilder sql = new StringBuilder("SELECT description, title, genre, id FROM movie");
        StringBuilder whereClause = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if (StringUtils.hasText(title)) {
            addWhereClause(whereClause, "title LIKE ?");
            params.add("%" + title + "%");
        }
        if (StringUtils.hasText(description)) {
            addWhereClause(whereClause, "description LIKE ?");
            params.add("%" + description + "%");
        }
        if (StringUtils.hasText(genre)) {
            addWhereClause(whereClause, "genre LIKE ?");
            params.add("%" + genre + "%");
        }
        if (StringUtils.hasText(id)) {
            addWhereClause(whereClause, "id = ?");
            params.add(id);
        }

        sql.append(whereClause);
        LOG.debug("Executing SQL: {}", sql);

        return this.jdbcTemplate.query(sql.toString(), params.toArray(), new RowMapper<MovieDto>() {
            @Override
            public MovieDto mapRow(ResultSet rs, int rowNum) throws SQLException {
                MovieDto ret = new MovieDto();
                ret.setDescription(rs.getString("description"));
                ret.setTitle(rs.getString("title"));
                ret.setGenre(rs.getString("genre"));
                ret.setId(rs.getString("id"));
                return ret;
            }
        });
    }

    private void addWhereClause(StringBuilder sb, String condition) {
        if (sb.length() == 0) {
            sb.append(" WHERE ").append(condition);
        } else {
            sb.append(" AND ").append(condition);
        }
    }

    public Movie saveMovieFromXml(String xml) {
        try {
            // Protect against XXE
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setNamespaceAware(true);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
            Element root = doc.getDocumentElement();

            Movie m = new Movie();
            m.setTitle(getText(root, "title"));
            m.setDescription(getText(root, "description"));
            m.setGenre(getText(root, "genre"));

            movieAutoDao.save(m);
            return m;
        } catch (Exception ex) {
            LOG.error("Error while saving movie from XML", ex);
            throw new RuntimeException("Invalid XML data", ex);
        }
    }

    private String getText(Element el, String tagName) {
        NodeList nl = el.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            NodeList children = nl.item(0).getChildNodes();
            if (children != null && children.getLength() > 0) {
                return children.item(0).getTextContent();
            }
        }
        LOG.debug("No text content for tag: {}", tagName);
        return null;
    }
}
