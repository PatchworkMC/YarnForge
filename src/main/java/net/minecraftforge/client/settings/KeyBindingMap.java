/*
 * Minecraft Forge
 * Copyright (c) 2016-2020.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.client.settings;

import javax.annotation.Nullable;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyBindingMap
{
    private static final EnumMap<KeyModifier, Map<InputUtil.Key, Collection<KeyBinding>>> map = new EnumMap<>(KeyModifier.class);
    static
    {
        for (KeyModifier modifier : KeyModifier.values())
        {
            map.put(modifier, new HashMap<>());
        }
    }

    @Nullable
    public KeyBinding lookupActive(InputUtil.Key keyCode)
    {
        KeyModifier activeModifier = KeyModifier.getActiveModifier();
        if (!activeModifier.matches(keyCode))
        {
            KeyBinding binding = getBinding(keyCode, activeModifier);
            if (binding != null)
            {
                return binding;
            }
        }
        return getBinding(keyCode, KeyModifier.NONE);
    }

    @Nullable
    private KeyBinding getBinding(InputUtil.Key keyCode, KeyModifier keyModifier)
    {
        Collection<KeyBinding> bindings = map.get(keyModifier).get(keyCode);
        if (bindings != null)
        {
            for (KeyBinding binding : bindings)
            {
                if (binding.isActiveAndMatches(keyCode))
                {
                    return binding;
                }
            }
        }
        return null;
    }

    public List<KeyBinding> lookupAll(InputUtil.Key keyCode)
    {
        List<KeyBinding> matchingBindings = new ArrayList<KeyBinding>();
        for (Map<InputUtil.Key, Collection<KeyBinding>> bindingsMap : map.values())
        {
            Collection<KeyBinding> bindings = bindingsMap.get(keyCode);
            if (bindings != null)
            {
                matchingBindings.addAll(bindings);
            }
        }
        return matchingBindings;
    }

    public void addKey(InputUtil.Key keyCode, KeyBinding keyBinding)
    {
        KeyModifier keyModifier = keyBinding.getKeyModifier();
        Map<InputUtil.Key, Collection<KeyBinding>> bindingsMap = map.get(keyModifier);
        Collection<KeyBinding> bindingsForKey = bindingsMap.get(keyCode);
        if (bindingsForKey == null)
        {
            bindingsForKey = new ArrayList<KeyBinding>();
            bindingsMap.put(keyCode, bindingsForKey);
        }
        bindingsForKey.add(keyBinding);
    }

    public void removeKey(KeyBinding keyBinding)
    {
        KeyModifier keyModifier = keyBinding.getKeyModifier();
        InputUtil.Key keyCode = keyBinding.getKey();
        Map<InputUtil.Key, Collection<KeyBinding>> bindingsMap = map.get(keyModifier);
        Collection<KeyBinding> bindingsForKey = bindingsMap.get(keyCode);
        if (bindingsForKey != null)
        {
            bindingsForKey.remove(keyBinding);
            if (bindingsForKey.isEmpty())
            {
                bindingsMap.remove(keyCode);
            }
        }
    }

    public void clearMap()
    {
        for (Map<InputUtil.Key, Collection<KeyBinding>> bindings : map.values())
        {
            bindings.clear();
        }
    }
}
