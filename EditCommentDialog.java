package com.atlassian.jira.pageobjects.pages.viewissue;

import com.atlassian.jira.pageobjects.components.SecurityLevelSelect;
import com.atlassian.jira.pageobjects.components.userpicker.MentionsUserPicker;
import com.atlassian.jira.pageobjects.dialogs.FormDialog;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedQuery;
import org.openqa.selenium.By;

import javax.inject.Inject;

/**
 * Represents edit comment dialog on view issue page.
 *
 * @since v5.0
 */
public class EditCommentDialog extends FormDialog implements Mentionable {
    @Inject
    protected PageBinder pageBinder;

    @ElementBy(cssSelector = "#comment-edit #comment")
    protected PageElement comment;

    public EditCommentDialog() {
        super("edit-comment");
    }

    public boolean submit() {
        return super.submit(By.id("comment-edit-submit"));
    }

    public SecurityLevelSelect getSecurityLevelControl() {
        return pageBinder.bind(SecurityLevelSelect.class, this.find(By.className("security-level")));
    }

    @Override
    public EditCommentDialog typeInput(CharSequence... text) {
        javascriptExecutor.executeScript("arguments[0].scrollIntoView();", this.comment.asWebElement());

        this.comment.type(text);
        return this;
    }

    /**
     * @deprecated Use {@link #typeInput(CharSequence...)}
     */
    @Deprecated
    public void setComment(String comment) {
        typeInput(comment);
    }

    @Override
    public String getInput() {
        return this.comment.getValue();
    }

    @Override
    public MentionsUserPicker getMentions() {
        return pageBinder.bind(MentionsUserPicker.class, this.comment);
    }

    @Override
    public TimedQuery<String> getInputTimed() {
        return this.comment.timed().getValue();
    }

    @Override
    public EditCommentDialog selectMention(String userId) {
        Poller.waitUntilTrue(getMentions().hasSuggestion(userId));
        getMentions().getSuggestion(userId).click();
        return this;
    }
}
