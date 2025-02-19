/**
 * Copyright (C) 2008 Torch Mobile Inc. All rights reserved. (http://www.torchmobile.com/)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 *
 */

#include "config.h"

#if ENABLE(WML)
#include "WMLAnchorElement.h"

#include "EventNames.h"
#include "KeyboardEvent.h"
#include "WMLTaskElement.h"
#include "HTMLNames.h"

namespace WebCore {

WMLAnchorElement::WMLAnchorElement(const QualifiedName& tagName, Document* doc)
    : WMLAElement(tagName, doc)
    , m_task(0)
{
    // Calling setIsLink(), and returning a non-null value on CSSStyleSelectors' linkAttribute
    // method, makes it possible to 'appear as link' (just like <a href="..">) without the need to
    // actually set the href value to an empty value in the DOM tree.
    setIsLink();
}

PassRefPtr<WMLAnchorElement> WMLAnchorElement::create(const QualifiedName& tagName, Document* document)
{
    return adoptRef(new WMLAnchorElement(tagName, document));
}

WMLAnchorElement::~WMLAnchorElement()
{
}

void WMLAnchorElement::defaultEventHandler(Event* event)
{
    bool shouldHandle = false;

    if (event->type() == eventNames().clickEvent)
        shouldHandle = true;
    else if (event->type() == eventNames().keydownEvent && event->isKeyboardEvent() && focused())
        shouldHandle = static_cast<KeyboardEvent*>(event)->keyIdentifier() == "Enter";

    if (shouldHandle && m_task) {
        m_task->executeTask();
        event->setDefaultHandled();
        return;
    }

    // Skip WMLAElement::defaultEventHandler, we don't own a href attribute, that needs to be handled.
    WMLElement::defaultEventHandler(event); 
}

void WMLAnchorElement::registerTask(WMLTaskElement* task)
{
    ASSERT(!m_task);
    m_task = task;
}

void WMLAnchorElement::deregisterTask(WMLTaskElement* task)
{
    ASSERT_UNUSED(task, m_task == task);
    m_task = 0;
}

// M+ extract href for go element.
KURL WMLAnchorElement::href() const
{
		if(m_task == 0)
			return KURL();
    return m_task->href();
}
// M-

}

#endif
